package com.epic.aiexpensevoice.presentation.state

data class ProfileUiState(
    val name: String = "Expense Companion",
    val email: String = "",
    val baseUrl: String = "",
    val isSaving: Boolean = false,
    val infoMessage: String? = null,
    val errorMessage: String? = null,
)
