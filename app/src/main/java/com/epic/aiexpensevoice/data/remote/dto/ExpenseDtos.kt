package com.epic.aiexpensevoice.data.remote.dto

data class ExpenseDto(
    val id: Int,
    val user_id: Int,
    val title: String,
    val amount: String,
    val category: String,
    val description: String?,
    val date: String,
)

data class CreateExpenseRequestDto(
    val title: String,
    val amount: Double,
    val category: String,
    val description: String? = null,
    val date: String? = null,
)

data class UpdateExpenseRequestDto(
    val amount: Double? = null,
    val category: String? = null,
    val title: String? = null,
    val description: String? = null,
    val date: String? = null,
)

data class UpdateExpenseResponseDto(
    val message: String,
    val expense: ExpenseDto,
)

data class DeleteExpenseResponseDto(
    val message: String,
)

data class ExpenseTotalDto(
    val total_expenses: String,
)
