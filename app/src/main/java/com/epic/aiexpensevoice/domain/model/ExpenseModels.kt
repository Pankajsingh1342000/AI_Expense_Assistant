package com.epic.aiexpensevoice.domain.model

import androidx.compose.ui.graphics.Color

data class ExpenseItem(
    val id: Int? = null,
    val title: String,
    val amount: Double,
    val category: String,
    val dateLabel: String,
    val description: String? = null,
)

data class CategorySpend(
    val category: String,
    val amount: Double,
    val share: Float,
    val color: Color,
)

enum class BudgetTone {
    Healthy,
    Watch,
    Risk,
}

data class BudgetStatus(
    val category: String,
    val spent: Double,
    val limit: Double,
    val statusLabel: String,
    val progress: Float,
    val tone: BudgetTone,
)

data class TrendPoint(
    val label: String,
    val amount: Double,
)

data class DashboardData(
    val totalSpent: Double,
    val totalChangeLabel: String,
    val topCategory: CategorySpend?,
    val categories: List<CategorySpend>,
    val budgets: List<BudgetStatus>,
    val recentExpenses: List<ExpenseItem>,
    val trendPoints: List<TrendPoint>,
    val insight: String,
    val summaryLabel: String,
) {
    val isEmpty: Boolean
        get() = totalSpent <= 0.0 &&
            topCategory == null &&
            categories.isEmpty() &&
            budgets.isEmpty() &&
            recentExpenses.isEmpty() &&
            trendPoints.isEmpty()
}

enum class Sender {
    User,
    Assistant,
}

data class ChatMessageUiModel(
    val id: String,
    val sender: Sender,
    val text: String,
    val blocks: List<AgentContentBlock> = emptyList(),
    val timestampLabel: String,
)

sealed interface AgentContentBlock {
    data class PlainText(val value: String) : AgentContentBlock
    data class Success(val title: String, val detail: String) : AgentContentBlock
    data class ExpenseList(val title: String, val expenses: List<ExpenseItem>) : AgentContentBlock
    data class TotalsSummary(val title: String, val amount: Double, val subtitle: String) : AgentContentBlock
    data class CategoryBreakdown(val title: String, val items: List<CategorySpend>) : AgentContentBlock
    data class BudgetOverview(val title: String, val budgets: List<BudgetStatus>) : AgentContentBlock
    data class Warning(val title: String, val detail: String) : AgentContentBlock
    data class Insight(val title: String, val detail: String) : AgentContentBlock
    data class Clarification(val title: String, val options: List<String>) : AgentContentBlock
    data class TrendChart(val title: String, val points: List<TrendPoint>) : AgentContentBlock
}
