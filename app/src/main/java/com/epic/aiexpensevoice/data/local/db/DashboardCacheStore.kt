package com.epic.aiexpensevoice.data.local.db

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.epic.aiexpensevoice.data.local.db.dao.DashboardCacheDao
import com.epic.aiexpensevoice.data.local.db.entity.DashboardCacheEntity
import com.epic.aiexpensevoice.domain.model.BudgetStatus
import com.epic.aiexpensevoice.domain.model.BudgetTone
import com.epic.aiexpensevoice.domain.model.CategorySpend
import com.epic.aiexpensevoice.domain.model.DashboardData
import com.epic.aiexpensevoice.domain.model.ExpenseItem
import com.epic.aiexpensevoice.domain.model.TrendPoint
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DashboardCacheStore(
    private val dashboardCacheDao: DashboardCacheDao,
    private val gson: Gson = Gson(),
) {
    fun observeDashboard(): Flow<DashboardData?> = dashboardCacheDao.observeCache().map { entity ->
        entity?.let(::fromEntity)
    }

    suspend fun saveDashboard(data: DashboardData) {
        dashboardCacheDao.upsert(
            DashboardCacheEntity(
                payloadJson = gson.toJson(data.toSnapshot()),
                updatedAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun clear() {
        dashboardCacheDao.clear()
    }

    private fun fromEntity(entity: DashboardCacheEntity): DashboardData =
        gson.fromJson(entity.payloadJson, DashboardSnapshot::class.java).toDomain()
}

private data class DashboardSnapshot(
    val totalSpent: Double,
    val totalChangeLabel: String,
    val topCategory: CategorySpendSnapshot?,
    val categories: List<CategorySpendSnapshot>,
    val budgets: List<BudgetStatusSnapshot>,
    val recentExpenses: List<ExpenseItemSnapshot>,
    val trendPoints: List<TrendPointSnapshot>,
    val insight: String,
    val summaryLabel: String,
)

private data class CategorySpendSnapshot(
    val category: String,
    val amount: Double,
    val share: Float,
    val colorArgb: Int,
)

private data class BudgetStatusSnapshot(
    val category: String,
    val spent: Double,
    val limit: Double,
    val statusLabel: String,
    val progress: Float,
    val tone: String,
)

private data class ExpenseItemSnapshot(
    val id: Int?,
    val title: String,
    val amount: Double,
    val category: String,
    val dateLabel: String,
    val description: String?,
)

private data class TrendPointSnapshot(
    val label: String,
    val amount: Double,
)

private fun DashboardData.toSnapshot(): DashboardSnapshot = DashboardSnapshot(
    totalSpent = totalSpent,
    totalChangeLabel = totalChangeLabel,
    topCategory = topCategory?.toSnapshot(),
    categories = categories.map(CategorySpend::toSnapshot),
    budgets = budgets.map(BudgetStatus::toSnapshot),
    recentExpenses = recentExpenses.map(ExpenseItem::toSnapshot),
    trendPoints = trendPoints.map(TrendPoint::toSnapshot),
    insight = insight,
    summaryLabel = summaryLabel,
)

private fun DashboardSnapshot.toDomain(): DashboardData = DashboardData(
    totalSpent = totalSpent,
    totalChangeLabel = totalChangeLabel,
    topCategory = topCategory?.toDomain(),
    categories = categories.map(CategorySpendSnapshot::toDomain),
    budgets = budgets.map(BudgetStatusSnapshot::toDomain),
    recentExpenses = recentExpenses.map(ExpenseItemSnapshot::toDomain),
    trendPoints = trendPoints.map(TrendPointSnapshot::toDomain),
    insight = insight,
    summaryLabel = summaryLabel,
)

private fun CategorySpend.toSnapshot(): CategorySpendSnapshot = CategorySpendSnapshot(
    category = category,
    amount = amount,
    share = share,
    colorArgb = color.toArgb(),
)

private fun CategorySpendSnapshot.toDomain(): CategorySpend = CategorySpend(
    category = category,
    amount = amount,
    share = share,
    color = Color(colorArgb),
)

private fun BudgetStatus.toSnapshot(): BudgetStatusSnapshot = BudgetStatusSnapshot(
    category = category,
    spent = spent,
    limit = limit,
    statusLabel = statusLabel,
    progress = progress,
    tone = tone.name,
)

private fun BudgetStatusSnapshot.toDomain(): BudgetStatus = BudgetStatus(
    category = category,
    spent = spent,
    limit = limit,
    statusLabel = statusLabel,
    progress = progress,
    tone = runCatching { BudgetTone.valueOf(tone) }.getOrDefault(BudgetTone.Healthy),
)

private fun ExpenseItem.toSnapshot(): ExpenseItemSnapshot = ExpenseItemSnapshot(
    id = id,
    title = title,
    amount = amount,
    category = category,
    dateLabel = dateLabel,
    description = description,
)

private fun ExpenseItemSnapshot.toDomain(): ExpenseItem = ExpenseItem(
    id = id,
    title = title,
    amount = amount,
    category = category,
    dateLabel = dateLabel,
    description = description,
)

private fun TrendPoint.toSnapshot(): TrendPointSnapshot = TrendPointSnapshot(
    label = label,
    amount = amount,
)

private fun TrendPointSnapshot.toDomain(): TrendPoint = TrendPoint(
    label = label,
    amount = amount,
)
