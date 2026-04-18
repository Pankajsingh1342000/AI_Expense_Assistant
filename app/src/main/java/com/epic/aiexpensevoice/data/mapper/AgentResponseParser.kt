package com.epic.aiexpensevoice.data.mapper

import androidx.compose.ui.graphics.Color
import com.epic.aiexpensevoice.domain.model.AgentContentBlock
import com.epic.aiexpensevoice.domain.model.BudgetStatus
import com.epic.aiexpensevoice.domain.model.BudgetTone
import com.epic.aiexpensevoice.domain.model.CategorySpend
import com.epic.aiexpensevoice.domain.model.DashboardData
import com.epic.aiexpensevoice.domain.model.ExpenseItem
import com.epic.aiexpensevoice.domain.model.TrendPoint
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlin.math.roundToInt

data class ParsedAgentResponse(
    val headline: String,
    val blocks: List<AgentContentBlock>,
)

class AgentResponseParser {
    private val palette = listOf(
        Color(0xFF18C29C),
        Color(0xFF5A9CFF),
        Color(0xFFF3A63C),
        Color(0xFFF06C78),
        Color(0xFF8A6AF8),
        Color(0xFF2AC5D9),
    )

    fun parseResponse(raw: String, query: String? = null): ParsedAgentResponse {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return ParsedAgentResponse(
                headline = "No response received",
                blocks = listOf(AgentContentBlock.Warning("Empty response", "The assistant returned an empty payload.")),
            )
        }

        val root = runCatching { JsonParser.parseString(trimmed) }.getOrNull()
        if (root == null) {
            return ParsedAgentResponse(trimmed, listOf(AgentContentBlock.PlainText(trimmed)))
        }
        if ((root.isJsonArray && root.asJsonArray.size() == 0) || (root.isJsonObject && root.asJsonObject.entrySet().isEmpty())) {
            return emptyStateForQuery(query)
        }

        val explicitReply = if (root.isJsonObject) {
            root.asJsonObject.findString("reply")
        } else null

        val texts = linkedSetOf<String>()
        val expenses = mutableListOf<ExpenseItem>()
        val categories = mutableListOf<CategorySpend>()
        val budgets = mutableListOf<BudgetStatus>()
        val trends = mutableListOf<TrendPoint>()
        val warnings = mutableListOf<String>()
        val totals = mutableListOf<AgentContentBlock.TotalsSummary>()
        val options = mutableListOf<String>()

        walk(root) { element ->
            if (!element.isJsonObject) return@walk
            collectFromObject(
                obj = element.asJsonObject,
                texts = texts,
                expenses = expenses,
                categories = categories,
                budgets = budgets,
                trends = trends,
                warnings = warnings,
                totals = totals,
                options = options,
            )
        }

        val normalizedCategories = normalizeCategoryShares(categories.distinctBy { it.category })
        val normalizedBudgets = budgets.distinctBy { it.category }
        val normalizedTrends = trends.distinctBy { it.label }
        val normalizedExpenses = expenses.distinctBy { "${it.title}-${it.amount}-${it.dateLabel}" }
        val headline = explicitReply
            ?: texts.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
            ?: "Assistant update ready"

        val blocks = buildList {
            if (normalizedExpenses.isNotEmpty()) add(AgentContentBlock.ExpenseList("Expenses", normalizedExpenses.take(8)))
            totals.distinctBy { "${it.title}-${it.amount}-${it.subtitle}" }.forEach(::add)
            if (normalizedCategories.isNotEmpty()) add(AgentContentBlock.CategoryBreakdown("Category breakdown", normalizedCategories))
            if (normalizedBudgets.isNotEmpty()) add(AgentContentBlock.BudgetOverview("Budget health", normalizedBudgets))
            if (normalizedTrends.isNotEmpty()) add(AgentContentBlock.TrendChart("Spending trend", normalizedTrends.takeLast(8)))
            
            // Only add texts as a separate block if we didn't use them for the headline,
            // or if there are multiple texts.
            val unusedTexts = texts.filter { it != headline }
            if (unusedTexts.isNotEmpty()) add(AgentContentBlock.PlainText(unusedTexts.joinToString("\n")))
            
            warnings.distinct().forEach { add(AgentContentBlock.Warning("Budget signal", it)) }
            if (options.isNotEmpty()) add(AgentContentBlock.Clarification("Try one of these", options.distinct()))
        }.ifEmpty {
            if (explicitReply == null) {
                listOf(AgentContentBlock.PlainText(headline))
            } else {
                emptyList() // Headline is enough if we have a direct reply
            }
        }

        return ParsedAgentResponse(headline = headline, blocks = blocks)
    }

    private fun emptyStateForQuery(query: String?): ParsedAgentResponse {
        val normalized = query.orEmpty().lowercase()
        return when {
            normalized.contains("expense") -> ParsedAgentResponse(
                headline = "No expenses found yet",
                blocks = listOf(
                    AgentContentBlock.Warning(
                        "No expenses found",
                        "You don't have any expenses yet. Try saying 'Add coffee 120' to get started.",
                    ),
                ),
            )
            normalized.contains("budget") -> ParsedAgentResponse(
                headline = "No budgets set yet",
                blocks = listOf(
                    AgentContentBlock.Warning(
                        "No budgets set",
                        "Set a budget first, for example 'Set food budget to 5000'.",
                    ),
                ),
            )
            normalized.contains("trend") || normalized.contains("category") || normalized.contains("summary") || normalized.contains("spending") -> ParsedAgentResponse(
                headline = "No analytics data yet",
                blocks = listOf(
                    AgentContentBlock.Warning(
                        "No analytics data",
                        "Add a few expenses first and then ask again for summaries, trends, or category totals.",
                    ),
                ),
            )
            else -> ParsedAgentResponse(
                headline = "No results found",
                blocks = listOf(AgentContentBlock.Warning("No results", "The assistant didn't find any matching data for that request.")),
            )
        }
    }

    fun buildDashboard(responses: Map<String, ParsedAgentResponse>): DashboardData {
        val blocks = responses.values.flatMap { it.blocks }
        val categoryItems = blocks.filterIsInstance<AgentContentBlock.CategoryBreakdown>()
            .flatMap { it.items }
            .filter { it.amount > 0 }
            .distinctBy { it.category }
        val categories = normalizeCategoryShares(categoryItems)

        val budgets = blocks.filterIsInstance<AgentContentBlock.BudgetOverview>()
            .flatMap { it.budgets }
            .filter { it.limit > 0 }
            .distinctBy { it.category }

        val recentExpenses = blocks.filterIsInstance<AgentContentBlock.ExpenseList>()
            .flatMap { it.expenses }
            .distinctBy { "${it.title}-${it.amount}-${it.dateLabel}" }
            .take(6)

        val trendPoints = blocks.filterIsInstance<AgentContentBlock.TrendChart>()
            .flatMap { it.points }
            .filter { it.amount > 0 }
            .distinctBy { it.label }

        val totalFromCards = blocks.filterIsInstance<AgentContentBlock.TotalsSummary>()
            .map { it.amount }
            .firstOrNull { it > 0 }
        val totalSpent = totalFromCards ?: categories.sumOf { it.amount }
        val topCategoryLabel = responses["top spending category"]?.blocks
            ?.filterIsInstance<AgentContentBlock.TotalsSummary>()
            ?.firstOrNull()
            ?.subtitle
        val topCategory = categories.firstOrNull { it.category.equals(topCategoryLabel, ignoreCase = true) }
            ?: categories.maxByOrNull { it.amount }
        val insight = cleanDashboardText(
            responses["give me insights"]?.headline
                ?: blocks.filterIsInstance<AgentContentBlock.PlainText>().lastOrNull()?.value,
        ) ?: "No insight available yet."

        return DashboardData(
            totalSpent = totalSpent,
            totalChangeLabel = cleanDashboardText(responses["monthly summary"]?.headline) ?: "No monthly activity yet",
            topCategory = topCategory?.takeIf { it.amount > 0 },
            categories = categories,
            budgets = budgets,
            recentExpenses = recentExpenses,
            trendPoints = trendPoints,
            insight = insight,
            summaryLabel = cleanDashboardText(responses["daily spending"]?.headline) ?: "No daily spending recorded yet.",
        )
    }

    private fun cleanDashboardText(value: String?): String? {
        val text = value?.trim().orEmpty()
        if (text.isBlank()) return null
        val normalized = text.lowercase()
        if (normalized == "assistant update ready") return null
        return text
    }

    private fun collectFromObject(
        obj: JsonObject,
        texts: MutableSet<String>,
        expenses: MutableList<ExpenseItem>,
        categories: MutableList<CategorySpend>,
        budgets: MutableList<BudgetStatus>,
        trends: MutableList<TrendPoint>,
        warnings: MutableList<String>,
        totals: MutableList<AgentContentBlock.TotalsSummary>,
        options: MutableList<String>,
    ) {
        // Find textual messages to display if the main `reply` wasn't set at the root
        obj.findString("reply", "message", "response", "answer", "summary", "insight")?.let(texts::add)
        obj.findString("warning", "alert", "error")?.let(warnings::add)
        parseTotals(obj)?.let(totals::add)
        parseExpense(obj)?.let(expenses::add)
        parseCategory(obj, categories.size)?.let(categories::add)
        parseBudget(obj)?.let(budgets::add)
        parseTrend(obj)?.let(trends::add)

        obj.entrySet().forEach { (key, value) ->
            if (key.contains("option", true) || key.contains("suggest", true)) {
                when {
                    value.isJsonPrimitive -> value.asStringOrNull()?.let(options::add)
                    value.isJsonArray -> value.asJsonArray.mapNotNull { option ->
                        option.asStringOrNull() ?: option.asJsonObjectOrNull()?.toOptionLabel()
                    }.forEach(options::add)
                }
            }
        }
    }

    private fun parseExpense(obj: JsonObject): ExpenseItem? {
        val title = obj.findString("title", "name", "merchant", "description") ?: return null
        val amount = obj.findNumber("amount", "spent", "value") ?: return null
        if (amount <= 0) return null
        return ExpenseItem(
            id = obj.findNumber("id")?.toInt(),
            title = title,
            amount = amount,
            category = obj.findString("category", "type") ?: "General",
            dateLabel = obj.findString("date", "created_at", "day") ?: "Recent",
            description = obj.findString("description"),
        )
    }

    private fun parseCategory(obj: JsonObject, index: Int): CategorySpend? {
        val category = obj.findString("category", "label", "name") ?: return null
        val amount = obj.findNumber("amount", "total", "spent", "month_total", "total_spent") ?: return null
        if (amount <= 0) return null
        return CategorySpend(
            category = category,
            amount = amount,
            share = obj.findNumber("share", "percentage", "percent", "usage_percent")?.div(100.0)?.toFloat() ?: 0f,
            color = palette[index % palette.size],
        )
    }

    private fun parseBudget(obj: JsonObject): BudgetStatus? {
        val category = obj.findString("category", "budget_name", "name") ?: return null
        val limit = obj.findNumber("limit", "budget", "target", "monthly_limit") ?: return null
        val spent = obj.findNumber("spent", "usage", "amount", "total_spent") ?: return null
        if (limit <= 0) return null
        val progress = obj.findNumber("usage_percent")?.div(100.0)?.toFloat()
            ?: (spent / limit).toFloat().coerceIn(0f, 1.4f)
        val tone = when {
            progress >= 1f -> BudgetTone.Risk
            progress >= 0.8f -> BudgetTone.Watch
            else -> BudgetTone.Healthy
        }
        return BudgetStatus(
            category = category,
            spent = spent,
            limit = limit,
            statusLabel = "${(progress * 100).roundToInt()}% used",
            progress = progress,
            tone = tone,
        )
    }

    private fun parseTrend(obj: JsonObject): TrendPoint? {
        val label = obj.findString("label", "day", "month", "period", "date") ?: return null
        val amount = obj.findNumber("amount", "total", "spent", "value") ?: return null
        if (amount <= 0) return null
        return TrendPoint(label = label, amount = amount)
    }

    private fun parseTotals(obj: JsonObject): AgentContentBlock.TotalsSummary? {
        return when {
            obj.has("month_total") -> AgentContentBlock.TotalsSummary(
                title = "Monthly total",
                amount = obj.findNumber("month_total") ?: return null,
                subtitle = "Current month spending",
            )
            obj.has("total_expenses") -> AgentContentBlock.TotalsSummary(
                title = "Total expenses",
                amount = obj.findNumber("total_expenses") ?: return null,
                subtitle = "All recorded spend",
            )
            obj.has("total_spent") && !obj.has("category") -> AgentContentBlock.TotalsSummary(
                title = "Total spent",
                amount = obj.findNumber("total_spent") ?: return null,
                subtitle = obj.findString("date") ?: "Requested period",
            )
            obj.has("top_category") && obj.has("amount") -> AgentContentBlock.TotalsSummary(
                title = "Top category",
                amount = obj.findNumber("amount") ?: return null,
                subtitle = obj.findString("top_category") ?: "Highest spending area",
            )
            else -> null
        }
    }

    private fun normalizeCategoryShares(categories: List<CategorySpend>): List<CategorySpend> {
        if (categories.isEmpty()) return emptyList()
        val total = categories.sumOf { it.amount }.takeIf { it > 0 } ?: return emptyList()
        return categories.map { item -> item.copy(share = (item.amount / total).toFloat()) }
    }

    private fun walk(element: JsonElement, visitor: (JsonElement) -> Unit) {
        visitor(element)
        when {
            element.isJsonArray -> element.asJsonArray.forEach { walk(it, visitor) }
            element.isJsonObject -> element.asJsonObject.entrySet().forEach { walk(it.value, visitor) }
        }
    }

    private fun JsonObject.findString(vararg keys: String): String? = keys.firstNotNullOfOrNull { target ->
        entrySet().firstOrNull { it.key.equals(target, ignoreCase = true) }?.value?.asStringOrNull()
    }

    private fun JsonObject.findNumber(vararg keys: String): Double? = keys.firstNotNullOfOrNull { target ->
        entrySet().firstOrNull { it.key.equals(target, ignoreCase = true) }?.value?.asDoubleOrNull()
    }

    private fun JsonElement.asStringOrNull(): String? = runCatching {
        when {
            isJsonPrimitive && asJsonPrimitive.isString -> asString
            isJsonPrimitive && asJsonPrimitive.isNumber -> asDouble.toString()
            else -> null
        }
    }.getOrNull()

    private fun JsonElement.asDoubleOrNull(): Double? = runCatching {
        when {
            isJsonPrimitive && asJsonPrimitive.isNumber -> asDouble
            isJsonPrimitive && asJsonPrimitive.isString -> asString.filter { it.isDigit() || it == '.' }.toDouble()
            else -> null
        }
    }.getOrNull()

    private fun JsonElement.asJsonObjectOrNull(): JsonObject? = if (isJsonObject) asJsonObject else null

    private fun JsonObject.toOptionLabel(): String? {
        val title = findString("title", "name", "merchant") ?: return null
        val amount = findNumber("amount")
        val date = findString("date")
        return listOfNotNull(
            title,
            amount?.let { "INR %.2f".format(it) },
            date,
        ).joinToString(" - ")
    }
}
