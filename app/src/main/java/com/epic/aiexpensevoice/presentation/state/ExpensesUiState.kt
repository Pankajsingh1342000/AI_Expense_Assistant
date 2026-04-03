package com.epic.aiexpensevoice.presentation.state

import com.epic.aiexpensevoice.domain.model.ExpenseItem

data class ExpensesUiState(
    val expenses: List<ExpenseItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)
