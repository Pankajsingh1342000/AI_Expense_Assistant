package com.epic.aiexpensevoice.data.repository

import androidx.compose.ui.graphics.Color
import com.epic.aiexpensevoice.core.common.asCurrency
import com.epic.aiexpensevoice.core.common.Resource
import com.epic.aiexpensevoice.data.local.db.DashboardCacheStore
import com.epic.aiexpensevoice.data.local.db.LocalExpenseStore
import com.epic.aiexpensevoice.data.remote.dto.BudgetDto
import com.epic.aiexpensevoice.data.remote.dto.ExpenseDto
import com.epic.aiexpensevoice.data.remote.network.ApiService
import com.epic.aiexpensevoice.domain.model.BudgetStatus
import com.epic.aiexpensevoice.domain.model.BudgetTone
import com.epic.aiexpensevoice.domain.model.CategorySpend
import com.epic.aiexpensevoice.domain.model.DashboardData
import com.epic.aiexpensevoice.domain.model.ExpenseItem
import com.epic.aiexpensevoice.domain.model.TrendPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class AnalyticsRepository(
    private val apiService: ApiService,
    private val dashboardCacheStore: DashboardCacheStore,
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

    fun observeCachedDashboard(): Flow<DashboardData?> = combine(
        dashboardCacheStore.observeDashboard(),
        localExpenseStore.observeDerivedDashboard(),
        localExpenseStore.observePendingCount(),
    ) { cached, localDerived, pendingCount ->
        when {
            pendingCount > 0 && !localDerived.isEmpty -> localDerived
            cached != null && !cached.isEmpty -> cached
            !localDerived.isEmpty -> localDerived
            else -> cached ?: localDerived
        }
    }

    suspend fun refreshDashboard(): Resource<DashboardData> = runCatching {
        val expenses = apiService.getExpenses()
            .requireBody("expenses")
            .map { it.toDomain() }

        localExpenseStore.replaceSyncedExpenses(expenses)

        val budgets = runCatching {
            apiService.getBudgets()
                .requireBody("budgets")
        }.getOrDefault(emptyList())

        buildDashboard(expenses = expenses, budgets = budgets)
            .also { dashboardCacheStore.saveDashboard(it) }
    }.fold(
        onSuccess = { Resource.Success(it) },
        onFailure = { error ->
            if (error is IOException) {
                Resource.Error("Unable to reach the service.", error)
            } else {
                Resource.Error(error.message ?: "Unable to load dashboard.", error)
            }
        },
    )

    private fun buildDashboard(
        expenses: List<ExpenseItem>,
        budgets: List<BudgetDto>,
    ): DashboardData {
        val now = LocalDate.now()
        val parsedExpenses = expenses.map { it to it.dateLabel.toLocalDateOrNow() }
        val monthExpenses = parsedExpenses.filter { (_, date) ->
            date.year == now.year && date.month == now.month
        }.map { it.first }

        val totalSpent = monthExpenses.sumOf { it.amount }
        val todaySpent = parsedExpenses
            .filter { (_, date) -> date == now }
            .sumOf { it.first.amount }

        val categories = monthExpenses.groupBy { it.category.lowercase() }
            .entries
            .sortedByDescending { it.value.sumOf { expense -> expense.amount } }
            .mapIndexed { index, entry ->
                CategorySpend(
                    category = entry.key.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                    amount = entry.value.sumOf { expense -> expense.amount },
                    share = 0f,
                    color = palette[index % palette.size],
                )
            }
            .normalizeShares()

        val monthExpenseGroups = monthExpenses.groupBy { it.dateLabel.toLocalDateOrNow() }
        val trend = (6 downTo 0).map { offset ->
            val day = now.minusDays(offset.toLong())
            TrendPoint(
                label = day.format(DateTimeFormatter.ofPattern("dd MMM")),
                amount = monthExpenseGroups[day].orEmpty().sumOf { expense -> expense.amount },
            )
        }

        val budgetStatuses = budgets.map { budget ->
            val spent = monthExpenses
                .filter { it.category.equals(budget.category, ignoreCase = true) }
                .sumOf { it.amount }
            val progress = if (budget.monthly_limit > 0) (spent / budget.monthly_limit).toFloat().coerceIn(0f, 1.4f) else 0f
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

        val topCategory = categories.maxByOrNull { it.amount }
        val insight = when {
            topCategory == null -> "No insight available yet."
            topCategory.share >= 0.5f -> "${topCategory.category} makes up most of your spending this month."
            budgetStatuses.any { it.tone == BudgetTone.Risk } -> "One or more budgets are already over their limit."
            budgetStatuses.any { it.tone == BudgetTone.Watch } -> "A budget is getting close to its limit."
            else -> "${topCategory.category} is your biggest spending category this month."
        }

        return DashboardData(
            totalSpent = totalSpent,
            totalChangeLabel = if (totalSpent > 0) "Month to date" else "No activity this month yet",
            topCategory = topCategory,
            categories = categories,
            budgets = budgetStatuses,
            recentExpenses = expenses.sortedByDescending { it.dateLabel.toSortableEpoch() }.take(6),
            trendPoints = trend,
            insight = insight,
            summaryLabel = if (todaySpent > 0) "Today: ${todaySpent.asCurrency()}" else "No spending recorded today",
        )
    }

    private fun List<CategorySpend>.normalizeShares(): List<CategorySpend> {
        val total = sumOf { it.amount }.takeIf { it > 0 } ?: return emptyList()
        return map { it.copy(share = (it.amount / total).toFloat()) }
    }

    private fun ExpenseDto.toDomain(): ExpenseItem = ExpenseItem(
        id = id,
        title = title,
        amount = amount.toDoubleOrNull() ?: 0.0,
        category = category,
        dateLabel = date,
        description = description,
    )
}

private fun <T> retrofit2.Response<T>.requireBody(label: String): T {
    val body = body()
    if (!isSuccessful || body == null) error("$label request failed with ${code()}")
    return body
}

private fun String.toLocalDateOrNow(): LocalDate = runCatching {
    when {
        contains("T") && contains("+") -> OffsetDateTime.parse(this).toLocalDate()
        contains("T") -> LocalDateTime.parse(this).toLocalDate()
        length >= 10 -> LocalDate.parse(take(10))
        else -> LocalDate.now()
    }
}.getOrElse { LocalDate.now() }

private fun String.toSortableEpoch(): Long = runCatching {
    when {
        contains("T") && contains("+") -> OffsetDateTime.parse(this).toInstant().toEpochMilli()
        contains("T") && contains("Z") -> OffsetDateTime.parse(this).toInstant().toEpochMilli()
        contains("T") -> LocalDateTime.parse(this).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        length >= 10 -> LocalDate.parse(take(10)).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        else -> Long.MIN_VALUE
    }
}.getOrDefault(Long.MIN_VALUE)
