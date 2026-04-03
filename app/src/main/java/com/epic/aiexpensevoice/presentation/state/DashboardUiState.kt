package com.epic.aiexpensevoice.presentation.state

import com.epic.aiexpensevoice.domain.model.DashboardData

data class DashboardUiState(
    val isLoading: Boolean = true,
    val data: DashboardData? = null,
    val errorMessage: String? = null,
    val isShowingCachedData: Boolean = false,
)
