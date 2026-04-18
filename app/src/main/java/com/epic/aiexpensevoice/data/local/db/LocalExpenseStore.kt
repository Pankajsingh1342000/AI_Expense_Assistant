package com.epic.aiexpensevoice.data.local.db

import com.epic.aiexpensevoice.data.local.db.dao.ExpenseCacheDao
import com.epic.aiexpensevoice.data.local.db.dao.PendingSyncDao
import com.epic.aiexpensevoice.data.local.db.entity.ExpenseCacheEntity
import com.epic.aiexpensevoice.data.local.db.entity.PendingSyncEntity
import com.epic.aiexpensevoice.core.common.asCurrency
import com.epic.aiexpensevoice.domain.model.BudgetStatus
import com.epic.aiexpensevoice.domain.model.BudgetTone
import com.epic.aiexpensevoice.domain.model.CategorySpend
import com.epic.aiexpensevoice.domain.model.DashboardData
import com.epic.aiexpensevoice.domain.model.ExpenseItem
import com.epic.aiexpensevoice.domain.model.TrendPoint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

class LocalExpenseStore(
    private val expenseCacheDao: ExpenseCacheDao,
    private val pendingSyncDao: PendingSyncDao,
) {
    fun observeExpenses(): Flow<List<ExpenseItem>> = expenseCacheDao.observeExpenses().map { entities ->
        entities.map(ExpenseCacheEntity::toDomain)
    }

    fun observeDerivedDashboard(): Flow<DashboardData> = observeExpenses().map(::buildDashboardFromExpenses)

    fun observePendingCount(): Flow<Int> = pendingSyncDao.observePendingCount()

    suspend fun getExpenses(): List<ExpenseItem> = expenseCacheDao.getExpenses().map(ExpenseCacheEntity::toDomain)

    suspend fun replaceSyncedExpenses(expenses: List<ExpenseItem>) {
        expenseCacheDao.deleteSyncedExpenses()
        val syncedEntities = expenses.mapIndexed { index, item ->
            ExpenseCacheEntity(
                cacheKey = buildSyncedKey(item, index),
                remoteId = item.id,
                title = item.title,
                amount = item.amount,
                category = item.category,
                dateIso = item.dateLabel,
                description = item.description,
                isPendingSync = false,
            )
        }
        expenseCacheDao.upsertAll(syncedEntities)
    }

    suspend fun upsertSyncedExpense(expense: ExpenseItem) {
        expenseCacheDao.upsert(
            ExpenseCacheEntity(
                cacheKey = buildSyncedKey(expense, 0),
                remoteId = expense.id,
                title = expense.title,
                amount = expense.amount,
                category = expense.category,
                dateIso = expense.dateLabel,
                description = expense.description,
                isPendingSync = false,
            ),
        )
    }

    suspend fun queueOfflineExpense(query: String, expense: ExpenseItem): ExpenseItem {
        val cacheKey = "local:${UUID.randomUUID()}"
        expenseCacheDao.upsert(
            ExpenseCacheEntity(
                cacheKey = cacheKey,
                remoteId = null,
                title = expense.title,
                amount = expense.amount,
                category = expense.category,
                dateIso = expense.dateLabel,
                description = expense.description,
                isPendingSync = true,
            ),
        )
        pendingSyncDao.enqueue(
            PendingSyncEntity(
                query = query,
                localExpenseKey = cacheKey,
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        return expense
    }

    suspend fun enqueuePendingQuery(query: String) {
        pendingSyncDao.enqueue(
            PendingSyncEntity(
                query = query,
                localExpenseKey = null,
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun getPendingCommands(): List<PendingSyncEntity> = pendingSyncDao.getPendingCommands()

    suspend fun markPendingCommandSynced(command: PendingSyncEntity) {
        command.localExpenseKey?.let { expenseCacheDao.markPendingSynced(it) }
        pendingSyncDao.deleteById(command.id)
    }

    suspend fun removeSyncedExpense(expenseId: Int) {
        expenseCacheDao.deleteByRemoteId(expenseId)
    }

    suspend fun clearAll() {
        expenseCacheDao.clear()
        pendingSyncDao.clear()
    }

    suspend fun updateLocalExpenseAmount(expenseId: Int?, title: String, newAmount: Double): ExpenseItem? {
        val target = expenseCacheDao.getExpenses().firstOrNull {
            (expenseId != null && it.remoteId == expenseId) || it.title.equals(title, ignoreCase = true)
        } ?: return null
        val updated = target.copy(amount = newAmount, isPendingSync = true)
        expenseCacheDao.upsert(updated)
        pendingSyncDao.enqueue(
            PendingSyncEntity(
                query = "update $title $newAmount",
                localExpenseKey = updated.cacheKey,
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        return updated.toDomain()
    }

    suspend fun deleteLocalExpense(expenseId: Int?, title: String): ExpenseItem? {
        val target = expenseCacheDao.getExpenses().firstOrNull {
            (expenseId != null && it.remoteId == expenseId) || it.title.equals(title, ignoreCase = true)
        } ?: return null
        expenseCacheDao.deleteByKey(target.cacheKey)
        pendingSyncDao.enqueue(
            PendingSyncEntity(
                query = "delete $title",
                localExpenseKey = null,
                createdAtEpochMillis = System.currentTimeMillis(),
            ),
        )
        return target.toDomain()
    }

    fun buildDashboardFromExpenses(expenses: List<ExpenseItem>): DashboardData {
        val parsedExpenses = expenses.map { expense ->
            expense to expense.dateLabel.toLocalDateOrNow()
        }
        val monthExpenses = parsedExpenses.filter { (_, date) ->
            val now = LocalDate.now()
            date.month == now.month && date.year == now.year
        }.map { it.first }
        val totalSpent = monthExpenses.sumOf { it.amount }

        val categoryTotals = monthExpenses.groupBy { it.category.lowercase() }
            .entries
            .toList()
            .mapIndexed { index, entry ->
                CategorySpend(
                    category = entry.key.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                    amount = entry.value.sumOf { expense -> expense.amount },
                    share = 0f,
                    color = dashboardColors[index % dashboardColors.size],
                )
            }
            .sortedByDescending { category -> category.amount }
        val normalizedCategories = normalizeShares(categoryTotals)

        val groupedByDay = monthExpenses.groupBy { it.dateLabel.toLocalDateOrNow() }
        val trendPoints = (6 downTo 0).map { offset ->
            val day = LocalDate.now().minusDays(offset.toLong())
            TrendPoint(
                day.format(DateTimeFormatter.ofPattern("dd MMM")),
                groupedByDay[day].orEmpty().sumOf { expense -> expense.amount },
            )
        }

        val todayTotal = parsedExpenses.filter { (_, date) -> date == LocalDate.now() }.sumOf { it.first.amount }
        val topCategory = normalizedCategories.maxByOrNull { it.amount }

        return DashboardData(
            totalSpent = totalSpent,
            totalChangeLabel = if (monthExpenses.isEmpty()) "No monthly activity yet" else "Offline snapshot from local expenses",
            topCategory = topCategory,
            categories = normalizedCategories,
            budgets = emptyList<BudgetStatus>(),
            recentExpenses = expenses.sortedByDescending { it.dateLabel.toSortableEpoch() }.take(6),
            trendPoints = trendPoints,
            insight = when {
                topCategory == null -> "No insight available yet."
                else -> "${topCategory.category} is currently your biggest local spending category."
            },
            summaryLabel = if (todayTotal > 0) "Today: ${todayTotal.asCurrency()}" else "No daily spending recorded yet.",
        )
    }

    private fun buildSyncedKey(expense: ExpenseItem, index: Int): String =
        expense.id?.let { "synced:id:$it" } ?: "synced:${expense.title.lowercase()}:${expense.amount}:${expense.dateLabel}:$index"

    private fun normalizeShares(categories: List<CategorySpend>): List<CategorySpend> {
        if (categories.isEmpty()) return emptyList()
        val total = categories.sumOf { it.amount }.takeIf { it > 0 } ?: return emptyList()
        return categories.map { it.copy(share = (it.amount / total).toFloat()) }
    }

    companion object {
        private val dashboardColors = listOf(
            androidx.compose.ui.graphics.Color(0xFF18C29C),
            androidx.compose.ui.graphics.Color(0xFF5A9CFF),
            androidx.compose.ui.graphics.Color(0xFFF3A63C),
            androidx.compose.ui.graphics.Color(0xFFF06C78),
            androidx.compose.ui.graphics.Color(0xFF8A6AF8),
            androidx.compose.ui.graphics.Color(0xFF2AC5D9),
        )
    }
}

private fun ExpenseCacheEntity.toDomain(): ExpenseItem = ExpenseItem(
    id = remoteId,
    title = title,
    amount = amount,
    category = category,
    dateLabel = dateIso,
    description = description,
)

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
