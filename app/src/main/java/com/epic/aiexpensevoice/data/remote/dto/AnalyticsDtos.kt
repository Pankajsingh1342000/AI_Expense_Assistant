package com.epic.aiexpensevoice.data.remote.dto

data class CategoryBreakdownDto(
    val category: String,
    val total: Double,
)

data class CategoryAmountDto(
    val category: String,
    val amount: Double,
)

data class MonthlySummaryDto(
    val month_total: Double,
    val categories: List<CategoryAmountDto>,
)

data class DailySpendingDto(
    val date: String,
    val total_spent: Double,
)

data class TopCategoryDto(
    val top_category: String,
    val amount: Double,
)

data class CategoryTotalDto(
    val category: String,
    val total_spent: Double,
)

data class SpendingTrendDto(
    val date: String,
    val amount: Double,
)

data class InsightsDto(
    val insight: String,
)
