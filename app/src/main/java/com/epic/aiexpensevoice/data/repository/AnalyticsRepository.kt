package com.epic.aiexpensevoice.data.repository

import androidx.compose.ui.graphics.Color
import com.epic.aiexpensevoice.core.common.Resource
import com.epic.aiexpensevoice.data.local.db.DashboardCacheStore
import com.epic.aiexpensevoice.data.local.db.LocalExpenseStore
import com.epic.aiexpensevoice.data.remote.dto.BudgetStatusDto
import com.epic.aiexpensevoice.data.remote.dto.BudgetWarningDto
import com.epic.aiexpensevoice.data.remote.dto.ExpenseDto
import com.epic.aiexpensevoice.data.remote.dto.MonthlySummaryDto
import com.epic.aiexpensevoice.data.remote.dto.SpendingTrendDto
import com.epic.aiexpensevoice.data.remote.dto.TopCategoryDto
import com.epic.aiexpensevoice.data.remote.network.ApiService
import com.epic.aiexpensevoice.domain.model.BudgetStatus
import com.epic.aiexpensevoice.domain.model.BudgetTone
import com.epic.aiexpensevoice.domain.model.CategorySpend
import com.epic.aiexpensevoice.domain.model.DashboardData
import com.epic.aiexpensevoice.domain.model.ExpenseItem
import com.epic.aiexpensevoice.domain.model.TrendPoint
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.io.IOException
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
        coroutineScope {
            val expensesDeferred = async { apiService.getExpenses() }
            val totalDeferred = async { apiService.getExpenseTotal() }
            val monthlyDeferred = async { apiService.getMonthlySummary() }
            val dailyDeferred = async { apiService.getDailySpending() }
            val topDeferred = async { apiService.getTopCategory() }
            val trendDeferred = async { apiService.getSpendingTrend() }
            val budgetsDeferred = async { apiService.getBudgets() }
            val warningsDeferred = async { apiService.getBudgetWarnings() }
            val insightsDeferred = async { apiService.getInsights() }

            val expenses = expensesDeferred.await().requireBody("expenses").map { it.toDomain() }
            localExpenseStore.replaceSyncedExpenses(expenses)

            val total = totalDeferred.await().requireBody("total").total_expenses.toDoubleOrNull() ?: 0.0
            val monthly = monthlyDeferred.await().requireBody("monthly summary")
            val daily = dailyDeferred.await().requireBody("daily spending")
            val top = topDeferred.await().requireBody("top category")
            val trend = trendDeferred.await().requireBody("spending trend")
            val budgets = budgetsDeferred.await().requireBody("budgets")
            val warnings = warningsDeferred.await().requireBody("budget warnings").warnings
            val insights = insightsDeferred.await().requireBody("insights")

            buildDashboard(
                total = total,
                monthly = monthly,
                dailySpent = daily.total_spent,
                top = top,
                trend = trend,
                expenses = expenses,
                budgetStatuses = budgets.map { budget ->
                    val warning = warnings.firstOrNull { it.category.equals(budget.category, ignoreCase = true) }
                    val statusDto = BudgetStatusDto(
                        category = budget.category,
                        budget = budget.monthly_limit,
                        spent = warning?.spent ?: 0.0,
                        remaining = budget.monthly_limit - (warning?.spent ?: 0.0),
                    )
                    statusDto.toDomain(warning)
                },
                insight = insights.insight,
            ).also { dashboardCacheStore.saveDashboard(it) }
        }
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
        total: Double,
        monthly: MonthlySummaryDto,
        dailySpent: Double,
        top: TopCategoryDto,
        trend: List<SpendingTrendDto>,
        expenses: List<ExpenseItem>,
        budgetStatuses: List<BudgetStatus>,
        insight: String,
    ): DashboardData {
        val categories = monthly.categories.mapIndexed { index, item ->
            CategorySpend(
                category = item.category.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                amount = item.amount,
                share = 0f,
                color = palette[index % palette.size],
            )
        }.normalizeShares()

        val normalizedTop = categories.firstOrNull { it.category.equals(top.top_category, ignoreCase = true) }
            ?: categories.maxByOrNull { it.amount }

        return DashboardData(
            totalSpent = total,
            totalChangeLabel = if (monthly.month_total > 0) "Month to date" else "No activity this month yet",
            topCategory = normalizedTop?.takeIf { it.amount > 0 },
            categories = categories.filter { it.amount > 0 },
            budgets = budgetStatuses.filter { it.limit > 0 },
            recentExpenses = expenses.sortedByDescending { it.dateLabel }.take(6),
            trendPoints = trend.map { TrendPoint(label = it.date.take(10), amount = it.amount) }.filter { it.amount > 0 },
            insight = insight.ifBlank { "No insight available yet." },
            summaryLabel = if (dailySpent > 0) "Today: INR %.2f".format(dailySpent) else "No spending recorded today",
        )
    }
    private fun List<CategorySpend>.normalizeShares(): List<CategorySpend> {
        val total = sumOf { it.amount }.takeIf { it > 0 } ?: return emptyList()
        return map { it.copy(share = (it.amount / total).toFloat()) }
    }

    private fun BudgetStatusDto.toDomain(warning: BudgetWarningDto?): BudgetStatus {
        val progress = ((warning?.usage_percent ?: if (budget > 0) (spent / budget) * 100 else 0.0) / 100.0).toFloat().coerceIn(0f, 1.4f)
        val tone = when {
            progress >= 1f -> BudgetTone.Risk
            progress >= 0.8f -> BudgetTone.Watch
            else -> BudgetTone.Healthy
        }
        return BudgetStatus(
            category = category.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
            spent = spent,
            limit = budget,
            statusLabel = "${(progress * 100).roundToInt()}% used",
            progress = progress,
            tone = tone,
        )
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
