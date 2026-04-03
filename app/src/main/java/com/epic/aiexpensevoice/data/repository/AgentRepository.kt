package com.epic.aiexpensevoice.data.repository

import androidx.compose.ui.graphics.Color
import com.epic.aiexpensevoice.core.common.Resource
import com.epic.aiexpensevoice.data.local.db.LocalExpenseStore
import com.epic.aiexpensevoice.data.mapper.AgentResponseParser
import com.epic.aiexpensevoice.data.mapper.ParsedAgentResponse
import com.epic.aiexpensevoice.data.remote.dto.AgentQueryDto
import com.epic.aiexpensevoice.data.remote.dto.BudgetDto
import com.epic.aiexpensevoice.data.remote.dto.BudgetWarningDto
import com.epic.aiexpensevoice.data.remote.dto.CategoryBreakdownDto
import com.epic.aiexpensevoice.data.remote.dto.CategoryTotalDto
import com.epic.aiexpensevoice.data.remote.dto.CreateBudgetRequestDto
import com.epic.aiexpensevoice.data.remote.dto.CreateExpenseRequestDto
import com.epic.aiexpensevoice.data.remote.dto.DailySpendingDto
import com.epic.aiexpensevoice.data.remote.dto.ExpenseDto
import com.epic.aiexpensevoice.data.remote.dto.InsightsDto
import com.epic.aiexpensevoice.data.remote.dto.MonthlySummaryDto
import com.epic.aiexpensevoice.data.remote.dto.SpendingTrendDto
import com.epic.aiexpensevoice.data.remote.dto.TopCategoryDto
import com.epic.aiexpensevoice.data.remote.dto.UpdateBudgetRequestDto
import com.epic.aiexpensevoice.data.remote.dto.UpdateExpenseRequestDto
import com.epic.aiexpensevoice.data.remote.network.ApiService
import com.epic.aiexpensevoice.domain.model.AgentContentBlock
import com.epic.aiexpensevoice.domain.model.BudgetStatus
import com.epic.aiexpensevoice.domain.model.BudgetTone
import com.epic.aiexpensevoice.domain.model.CategorySpend
import com.epic.aiexpensevoice.domain.model.ExpenseItem
import com.epic.aiexpensevoice.domain.model.TrendPoint
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import java.time.OffsetDateTime
import java.util.Locale
import kotlin.math.roundToInt

class AgentRepository(
    private val apiService: ApiService,
    private val parser: AgentResponseParser,
    private val localExpenseStore: LocalExpenseStore,
) {
    private val palette = listOf(
        Color(0xFF18C29C),
        Color(0xFF5A9CFF),
        Color(0xFFF3A63C),
        Color(0xFFF06C78),
        Color(0xFF8A6AF8),
        Color(0xFF2AC5D9),
    )

    suspend fun sendQuery(query: String): Resource<ParsedAgentResponse> {
        val prompt = query.trim()
        return runCatching {
            handleDirectQuery(prompt) ?: handleAgentQuery(prompt)
        }.fold(
            onSuccess = { Resource.Success(it) },
            onFailure = { error ->
                if (error is IOException) {
                    handleOfflineFallback(prompt)
                } else {
                    Resource.Error(error.message ?: "Unable to reach the assistant.", error)
                }
            },
        )
    }

    fun observeLocalExpenses(): Flow<List<ExpenseItem>> = localExpenseStore.observeExpenses()

    suspend fun updateExpense(title: String, amount: Double): Resource<ParsedAgentResponse> =
        sendQuery("update $title $amount")

    suspend fun deleteExpense(title: String): Resource<ParsedAgentResponse> =
        sendQuery("delete $title")

    private suspend fun handleDirectQuery(query: String): ParsedAgentResponse? {
        val normalized = query.lowercase(Locale.getDefault()).trim()

        parseAddExpense(normalized, query)?.let { request ->
            val response = apiService.createExpense(request)
            val body = response.body()
            if (!response.isSuccessful || body == null) error("Expense request failed with ${response.code()}")
            val expense = body.toDomain()
            localExpenseStore.upsertSyncedExpense(expense)
            return ParsedAgentResponse(
                headline = "Expense added",
                blocks = listOf(
                    AgentContentBlock.Success("Expense added", "${expense.title} for INR %.2f".format(expense.amount)),
                    AgentContentBlock.ExpenseList("Recent", listOf(expense)),
                ),
            )
        }

        if (isListQuery(normalized)) {
            val response = apiService.getExpenses()
            val body = response.body()
            if (!response.isSuccessful || body == null) error("Expenses request failed with ${response.code()}")
            val expenses = body.map { it.toDomain() }
            localExpenseStore.replaceSyncedExpenses(expenses)
            return if (expenses.isEmpty()) {
                ParsedAgentResponse(
                    headline = "No expenses found yet",
                    blocks = listOf(AgentContentBlock.Warning("No expenses found", "Add your first expense to start tracking.")),
                )
            } else {
                ParsedAgentResponse(
                    headline = "Here are your expenses",
                    blocks = listOf(AgentContentBlock.ExpenseList("Expenses", expenses)),
                )
            }
        }

        parseUpdateExpense(normalized)?.let { (title, amount) ->
            val expense = resolveExpenseByTitle(title) ?: return ParsedAgentResponse(
                headline = "Expense not found",
                blocks = listOf(AgentContentBlock.Warning("Expense not found", "I couldn't find that expense to update.")),
            )
            if (expense.id != null) {
                val response = apiService.updateExpense(expense.id, UpdateExpenseRequestDto(amount = amount))
                val body = response.body()
                if (!response.isSuccessful || body == null) error("Expense update failed with ${response.code()}")
                val updated = body.expense.toDomain()
                localExpenseStore.upsertSyncedExpense(updated)
                return ParsedAgentResponse(
                    headline = body.message,
                    blocks = listOf(AgentContentBlock.Success("Expense updated", "${updated.title} is now INR %.2f".format(updated.amount))),
                )
            }
            val updated = localExpenseStore.updateLocalExpenseAmount(null, expense.title, amount)
            return ParsedAgentResponse(
                headline = "Updated offline",
                blocks = listOf(AgentContentBlock.Success("Expense updated", "${updated?.title ?: expense.title} was updated locally.")),
            )
        }

        parseDeleteExpense(normalized)?.let { title ->
            val expense = resolveExpenseByTitle(title) ?: return ParsedAgentResponse(
                headline = "Expense not found",
                blocks = listOf(AgentContentBlock.Warning("Expense not found", "I couldn't find that expense to delete.")),
            )
            if (expense.id != null) {
                val response = apiService.deleteExpense(expense.id)
                if (!response.isSuccessful) error("Expense delete failed with ${response.code()}")
                localExpenseStore.removeSyncedExpense(expense.id)
                return ParsedAgentResponse(
                    headline = response.body()?.message ?: "Expense deleted",
                    blocks = listOf(AgentContentBlock.Success("Expense deleted", expense.title)),
                )
            }
            localExpenseStore.deleteLocalExpense(null, expense.title)
            return ParsedAgentResponse(
                headline = "Deleted offline",
                blocks = listOf(AgentContentBlock.Success("Expense deleted", expense.title)),
            )
        }

        if (normalized == "monthly summary") {
            val monthly = apiService.getMonthlySummary().requireBody("monthly summary")
            return monthly.toParsedResponse()
        }

        if (normalized == "daily spending" || normalized == "today spending") {
            val daily = apiService.getDailySpending().requireBody("daily spending")
            return daily.toParsedResponse()
        }

        if (normalized == "top spending category") {
            val top = apiService.getTopCategory().requireBody("top category")
            return top.toParsedResponse()
        }

        if (normalized == "spending trend") {
            val trend = apiService.getSpendingTrend().requireBody("spending trend")
            return trend.toTrendParsedResponse()
        }

        if (normalized == "category breakdown") {
            val categories = apiService.getCategoryBreakdown().requireBody("category breakdown")
            return categories.toCategoryBreakdownResponse()
        }

        if (normalized == "give me insights" || normalized == "insights") {
            val insights = apiService.getInsights().requireBody("insights")
            return insights.toParsedResponse()
        }

        if (normalized == "budget overview" || normalized == "show all budgets") {
            val budgets = apiService.getBudgets().requireBody("budgets")
            val warnings = apiService.getBudgetWarnings().requireBody("budget warnings").warnings
            return budgets.toBudgetOverviewResponse(warnings)
        }

        if (normalized == "budget warning" || normalized == "any budget warning?" || normalized == "budget warnings") {
            val warnings = apiService.getBudgetWarnings().requireBody("budget warnings").warnings
            return warnings.toWarningsResponse()
        }

        parseBudgetStatusCategory(normalized)?.let { category ->
            val status = apiService.getBudgetStatus(category).requireBody("budget status")
            return status.toBudgetStatusResponse()
        }

        parseCategoryTotal(normalized)?.let { category ->
            val total = apiService.getCategoryTotal(category).requireBody("category total")
            return total.toParsedResponse()
        }

        parseSetBudget(normalized)?.let { (category, monthlyLimit) ->
            val budget = apiService.createBudget(CreateBudgetRequestDto(category, monthlyLimit)).requireBody("create budget")
            return ParsedAgentResponse(
                headline = "Budget saved",
                blocks = listOf(
                    AgentContentBlock.Success("Budget saved", "${budget.category.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} set to INR %.2f".format(budget.monthly_limit)),
                ),
            )
        }

        parseUpdateBudget(normalized)?.let { (category, monthlyLimit) ->
            val budget = apiService.updateBudget(category, UpdateBudgetRequestDto(monthlyLimit)).requireBody("update budget")
            return ParsedAgentResponse(
                headline = "Budget updated",
                blocks = listOf(
                    AgentContentBlock.Success("Budget updated", "${budget.category.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} set to INR %.2f".format(budget.monthly_limit)),
                ),
            )
        }

        return null
    }

    private suspend fun handleAgentQuery(query: String): ParsedAgentResponse {
        val response = apiService.sendAgentQuery(AgentQueryDto(query))
        val rawBody = response.body()?.string().orEmpty()
        if (!response.isSuccessful) error(rawBody.ifBlank { "Assistant request failed with ${response.code()}" })
        val parsed = parser.parseResponse(rawBody, query)
        persistSuccessfulResponse(query, parsed)
        syncPendingCommands()
        return parsed
    }

    private suspend fun persistSuccessfulResponse(query: String, parsed: ParsedAgentResponse) {
        val expenses = parsed.blocks.filterIsInstance<AgentContentBlock.ExpenseList>().flatMap { it.expenses }
        when {
            isListQuery(query) -> localExpenseStore.replaceSyncedExpenses(expenses)
            expenses.isNotEmpty() -> expenses.forEach { localExpenseStore.upsertSyncedExpense(it) }
        }
    }

    private suspend fun syncPendingCommands() {
        localExpenseStore.getPendingCommands().forEach { command ->
            val parsed = runCatching { handleDirectQuery(command.query) ?: handleAgentQuery(command.query) }.getOrNull() ?: return
            persistSuccessfulResponse(command.query, parsed)
            localExpenseStore.markPendingCommandSynced(command)
        }
    }

    private suspend fun handleOfflineFallback(query: String): Resource<ParsedAgentResponse> {
        parseOfflineAdd(query)?.let { expense ->
            val saved = localExpenseStore.queueOfflineExpense(query, expense)
            return Resource.Success(
                ParsedAgentResponse(
                    headline = "Saved offline",
                    blocks = listOf(
                        AgentContentBlock.Success("Expense queued", "${saved.title} will sync when you're back online."),
                        AgentContentBlock.ExpenseList("Offline expenses", listOf(saved)),
                    ),
                ),
            )
        }

        parseOfflineUpdate(query)?.let { (title, amount) ->
            val updated = localExpenseStore.updateLocalExpenseAmount(null, title, amount)
            return if (updated != null) {
                Resource.Success(
                    ParsedAgentResponse(
                        headline = "Updated offline",
                        blocks = listOf(AgentContentBlock.Success("Expense updated", "${updated.title} was updated locally and queued for sync.")),
                    ),
                )
            } else {
                Resource.Error("No matching local expense found to update while offline.")
            }
        }

        parseOfflineDelete(query)?.let { title ->
            val deleted = localExpenseStore.deleteLocalExpense(null, title)
            return if (deleted != null) {
                Resource.Success(
                    ParsedAgentResponse(
                        headline = "Deleted offline",
                        blocks = listOf(AgentContentBlock.Success("Expense removed", "${deleted.title} was removed locally and queued for sync.")),
                    ),
                )
            } else {
                Resource.Error("No matching local expense found to delete while offline.")
            }
        }

        if (isListQuery(query)) {
            val localExpenses = localExpenseStore.getExpenses()
            return Resource.Success(
                ParsedAgentResponse(
                    headline = if (localExpenses.isEmpty()) "No local expenses found" else "Showing offline expenses",
                    blocks = listOf(AgentContentBlock.ExpenseList("Offline expenses", localExpenses)),
                ),
            )
        }

        localExpenseStore.enqueuePendingQuery(query)
        return Resource.Success(
            ParsedAgentResponse(
                headline = "Queued offline",
                blocks = listOf(AgentContentBlock.Warning("Offline mode", "This request was queued and will sync when the network is available.")),
            ),
        )
    }

    private suspend fun resolveExpenseByTitle(title: String): ExpenseItem? {
        val local = localExpenseStore.getExpenses().firstOrNull { it.title.equals(title, ignoreCase = true) }
        if (local != null) return local
        val response = apiService.getExpenses()
        val body = response.body().orEmpty()
        if (response.isSuccessful) {
            val expenses = body.map { it.toDomain() }
            localExpenseStore.replaceSyncedExpenses(expenses)
            return expenses.firstOrNull { it.title.equals(title, ignoreCase = true) }
        }
        return null
    }

    private fun parseAddExpense(normalized: String, original: String): CreateExpenseRequestDto? {
        val match = Regex("""^\s*add\s+(.+?)\s+(\d+(?:\.\d+)?)\s*$""", RegexOption.IGNORE_CASE).find(original) ?: return null
        val title = match.groupValues[1].trim()
        val amount = match.groupValues[2].toDoubleOrNull() ?: return null
        return CreateExpenseRequestDto(
            title = title,
            amount = amount,
            category = inferCategory(title),
            description = null,
            date = OffsetDateTime.now().toString(),
        )
    }

    private fun parseOfflineAdd(query: String): ExpenseItem? {
        val match = Regex("""^\s*add\s+(.+?)\s+(\d+(?:\.\d+)?)\s*$""", RegexOption.IGNORE_CASE).find(query) ?: return null
        val title = match.groupValues[1].trim()
        val amount = match.groupValues[2].toDoubleOrNull() ?: return null
        return ExpenseItem(
            title = title.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
            amount = amount,
            category = inferCategory(title),
            dateLabel = OffsetDateTime.now().toString(),
            description = null,
        )
    }

    private fun parseUpdateExpense(query: String): Pair<String, Double>? {
        val match = Regex("""^\s*update\s+(.+?)\s+(\d+(?:\.\d+)?)\s*$""", RegexOption.IGNORE_CASE).find(query) ?: return null
        return match.groupValues[1].trim() to (match.groupValues[2].toDoubleOrNull() ?: return null)
    }

    private fun parseOfflineUpdate(query: String): Pair<String, Double>? = parseUpdateExpense(query)

    private fun parseDeleteExpense(query: String): String? {
        val match = Regex("""^\s*delete\s+(.+?)\s*$""", RegexOption.IGNORE_CASE).find(query) ?: return null
        return match.groupValues[1].trim()
    }

    private fun parseOfflineDelete(query: String): String? = parseDeleteExpense(query)

    private fun parseSetBudget(query: String): Pair<String, Double>? {
        val match = Regex("""^\s*set\s+([a-zA-Z ]+?)\s+budget(?:\s+to)?\s+(\d+(?:\.\d+)?)\s*$""", RegexOption.IGNORE_CASE).find(query) ?: return null
        return match.groupValues[1].trim().lowercase() to (match.groupValues[2].toDoubleOrNull() ?: return null)
    }

    private fun parseUpdateBudget(query: String): Pair<String, Double>? {
        val match = Regex("""^\s*update\s+([a-zA-Z ]+?)\s+budget(?:\s+to)?\s+(\d+(?:\.\d+)?)\s*$""", RegexOption.IGNORE_CASE).find(query) ?: return null
        return match.groupValues[1].trim().lowercase() to (match.groupValues[2].toDoubleOrNull() ?: return null)
    }

    private fun parseBudgetStatusCategory(query: String): String? {
        val match = Regex("""budget\s+status(?:\s+for)?\s+([a-zA-Z ]+)""", RegexOption.IGNORE_CASE).find(query) ?: return null
        return match.groupValues[1].trim().lowercase()
    }

    private fun parseCategoryTotal(query: String): String? {
        val match = Regex("""category\s+total(?:\s+for)?\s+([a-zA-Z ]+)""", RegexOption.IGNORE_CASE).find(query) ?: return null
        return match.groupValues[1].trim().lowercase()
    }

    private fun isListQuery(query: String): Boolean {
        val normalized = query.lowercase(Locale.getDefault())
        return normalized.contains("show my expenses") ||
            normalized.contains("what are my expenses") ||
            normalized.contains("list expenses") ||
            normalized == "expenses" ||
            normalized.contains("show expenses") ||
            normalized.contains("my expenses")
    }

    private fun inferCategory(title: String): String {
        val normalized = title.lowercase(Locale.getDefault())
        return when {
            normalized.contains("coffee") || normalized.contains("tea") -> "beverages"
            normalized.contains("uber") || normalized.contains("ola") || normalized.contains("metro") -> "transport"
            normalized.contains("swiggy") || normalized.contains("zomato") || normalized.contains("lunch") || normalized.contains("dinner") -> "food"
            normalized.contains("blinkit") || normalized.contains("zepto") || normalized.contains("grocery") -> "groceries"
            normalized.contains("electricity") || normalized.contains("rent") || normalized.contains("internet") -> "bills"
            else -> "misc"
        }
    }

    private fun ExpenseDto.toDomain(): ExpenseItem = ExpenseItem(
        id = id,
        title = title,
        amount = amount.toDoubleOrNull() ?: 0.0,
        category = category,
        dateLabel = date,
        description = description,
    )

    private fun MonthlySummaryDto.toParsedResponse(): ParsedAgentResponse {
        if (month_total <= 0 && categories.isEmpty()) {
            return ParsedAgentResponse("No monthly activity yet", listOf(AgentContentBlock.Warning("No activity", "Add expenses to start seeing your monthly summary.")))
        }
        val categoryBlocks = categories.mapIndexed { index, item ->
            CategorySpend(
                category = item.category.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                amount = item.amount,
                share = 0f,
                color = palette[index % palette.size],
            )
        }.normalizeShares()
        return ParsedAgentResponse(
            headline = "Monthly summary",
            blocks = listOf(
                AgentContentBlock.TotalsSummary("Monthly total", month_total, "Current month"),
                AgentContentBlock.CategoryBreakdown("Category breakdown", categoryBlocks),
            ),
        )
    }

    private fun DailySpendingDto.toParsedResponse(): ParsedAgentResponse = ParsedAgentResponse(
        headline = if (total_spent > 0) "Daily spending" else "No spending recorded today",
        blocks = listOf(AgentContentBlock.TotalsSummary("Daily total", total_spent, date)),
    )

    private fun TopCategoryDto.toParsedResponse(): ParsedAgentResponse {
        if (amount <= 0) {
            return ParsedAgentResponse("No expenses found yet", listOf(AgentContentBlock.Warning("No activity", "Add expenses to identify your top category.")))
        }
        return ParsedAgentResponse(
            headline = "Top spending category",
            blocks = listOf(AgentContentBlock.TotalsSummary("Top category", amount, top_category.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() })),
        )
    }

    private fun List<SpendingTrendDto>.toTrendParsedResponse(): ParsedAgentResponse {
        val points = filter { it.amount > 0 }.map { TrendPoint(it.date.take(10), it.amount) }
        return if (points.isEmpty()) {
            ParsedAgentResponse("No trend yet", listOf(AgentContentBlock.Warning("No trend yet", "Add a few expenses to see your spending trend.")))
        } else {
            ParsedAgentResponse("Spending trend", listOf(AgentContentBlock.TrendChart("Spending trend", points)))
        }
    }

    private fun List<CategoryBreakdownDto>.toCategoryBreakdownResponse(): ParsedAgentResponse {
        val items = filter { it.total > 0 }.mapIndexed { index, item ->
            CategorySpend(
                category = item.category.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                amount = item.total,
                share = 0f,
                color = palette[index % palette.size],
            )
        }.normalizeShares()
        return if (items.isEmpty()) {
            ParsedAgentResponse("No category data yet", listOf(AgentContentBlock.Warning("No category data", "Add expenses to see where your money is going.")))
        } else {
            ParsedAgentResponse("Category breakdown", listOf(AgentContentBlock.CategoryBreakdown("Category breakdown", items)))
        }
    }

    private fun InsightsDto.toParsedResponse(): ParsedAgentResponse = ParsedAgentResponse(
        headline = "AI insight",
        blocks = listOf(AgentContentBlock.Insight("AI insight", insight)),
    )

    private fun List<BudgetDto>.toBudgetOverviewResponse(warnings: List<BudgetWarningDto>): ParsedAgentResponse {
        val statuses = map { budget ->
            val warning = warnings.firstOrNull { it.category.equals(budget.category, ignoreCase = true) }
            val spent = warning?.spent ?: 0.0
            val progress = ((warning?.usage_percent ?: if (budget.monthly_limit > 0) (spent / budget.monthly_limit) * 100 else 0.0) / 100.0).toFloat().coerceIn(0f, 1.4f)
            val tone = when {
                progress >= 1f -> BudgetTone.Risk
                progress >= 0.8f -> BudgetTone.Watch
                else -> BudgetTone.Healthy
            }
            BudgetStatus(
                category = budget.category.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                spent = spent,
                limit = budget.monthly_limit,
                statusLabel = "${(progress * 100).roundToInt()}% used",
                progress = progress,
                tone = tone,
            )
        }.filter { it.limit > 0 }
        return if (statuses.isEmpty()) {
            ParsedAgentResponse("No budgets set yet", listOf(AgentContentBlock.Warning("No budgets set", "Set a category budget to start tracking limits.")))
        } else {
            ParsedAgentResponse("Budget overview", listOf(AgentContentBlock.BudgetOverview("Budget overview", statuses)))
        }
    }

    private fun List<BudgetWarningDto>.toWarningsResponse(): ParsedAgentResponse {
        if (isEmpty()) {
            return ParsedAgentResponse("No budget warnings", listOf(AgentContentBlock.Success("All good", "None of your budgets are near their limit right now.")))
        }
        return ParsedAgentResponse(
            headline = "Budget warning",
            blocks = map {
                AgentContentBlock.Warning(
                    it.category.replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase() else ch.toString() },
                    "${it.usage_percent.roundToInt()}% used",
                )
            },
        )
    }

    private fun com.epic.aiexpensevoice.data.remote.dto.BudgetStatusDto.toBudgetStatusResponse(): ParsedAgentResponse {
        val progress = if (budget > 0) (spent / budget).toFloat().coerceIn(0f, 1.4f) else 0f
        val tone = when {
            progress >= 1f -> BudgetTone.Risk
            progress >= 0.8f -> BudgetTone.Watch
            else -> BudgetTone.Healthy
        }
        return ParsedAgentResponse(
            headline = "Budget status",
            blocks = listOf(
                AgentContentBlock.BudgetOverview(
                    "Budget status",
                    listOf(
                        BudgetStatus(
                            category = category.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                            spent = spent,
                            limit = budget,
                            statusLabel = "${(progress * 100).roundToInt()}% used",
                            progress = progress,
                            tone = tone,
                        ),
                    ),
                ),
            ),
        )
    }

    private fun CategoryTotalDto.toParsedResponse(): ParsedAgentResponse = ParsedAgentResponse(
        headline = "Category total",
        blocks = listOf(
            AgentContentBlock.TotalsSummary(
                title = category.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                amount = total_spent,
                subtitle = "Total spent",
            ),
        ),
    )

    private fun List<CategorySpend>.normalizeShares(): List<CategorySpend> {
        val total = sumOf { it.amount }.takeIf { it > 0 } ?: return emptyList()
        return map { it.copy(share = (it.amount / total).toFloat()) }
    }
}

private fun <T> retrofit2.Response<T>.requireBody(label: String): T {
    val body = body()
    if (!isSuccessful || body == null) error("$label request failed with ${code()}")
    return body
}
