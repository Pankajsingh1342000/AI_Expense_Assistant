package com.epic.aiexpensevoice.data.remote.dto

data class BudgetDto(
    val category: String,
    val monthly_limit: Double,
    val id: Int,
    val user_id: Int,
)

data class CreateBudgetRequestDto(
    val category: String,
    val monthly_limit: Double,
)

data class UpdateBudgetRequestDto(
    val monthly_limit: Double,
)

data class BudgetStatusDto(
    val category: String,
    val budget: Double,
    val spent: Double,
    val remaining: Double,
)

data class BudgetWarningDto(
    val category: String,
    val spent: Double,
    val budget: Double,
    val usage_percent: Double,
)

data class BudgetWarningsResponseDto(
    val warnings: List<BudgetWarningDto>,
)
