package com.epic.aiexpensevoice.presentation.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epic.aiexpensevoice.core.common.Resource
import com.epic.aiexpensevoice.data.repository.AnalyticsRepository
import com.epic.aiexpensevoice.presentation.state.DashboardUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val analyticsRepository: AnalyticsRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        analyticsRepository.observeCachedDashboard()
            .onEach { cached ->
                if (cached != null) {
                    _uiState.value = _uiState.value.copy(
                        data = cached,
                        isShowingCachedData = true,
                    )
                }
            }
            .launchIn(viewModelScope)
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = analyticsRepository.refreshDashboard()) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    data = result.data,
                    errorMessage = null,
                    isShowingCachedData = false,
                )
                is Resource.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = if (_uiState.value.data != null) {
                        "We couldn't refresh your dashboard right now."
                    } else {
                        userFriendlyError(result.message)
                    },
                    isShowingCachedData = _uiState.value.data != null,
                )
                Resource.Loading -> Unit
            }
        }
    }

    private fun userFriendlyError(message: String): String {
        val normalized = message.lowercase()
        return when {
            "401" in normalized || "unauthorized" in normalized -> "Your session has expired. Please sign in again."
            "timeout" in normalized -> "Things are taking longer than expected. Please try again."
            "unable to reach" in normalized || "failed to connect" in normalized -> "You're offline or the service is unavailable right now."
            else -> "We couldn't load your dashboard right now."
        }
    }
}
