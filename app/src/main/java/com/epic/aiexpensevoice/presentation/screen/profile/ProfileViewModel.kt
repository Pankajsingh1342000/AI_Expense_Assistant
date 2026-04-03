package com.epic.aiexpensevoice.presentation.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epic.aiexpensevoice.core.common.Resource
import com.epic.aiexpensevoice.data.repository.AuthRepository
import com.epic.aiexpensevoice.presentation.state.ProfileUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        authRepository.session
            .onEach { session ->
                _uiState.value = _uiState.value.copy(
                    name = session.name ?: "Expense Companion",
                    email = session.email.orEmpty(),
                    baseUrl = session.baseUrlOverride ?: session.resolvedBaseUrl,
                )
            }
            .launchIn(viewModelScope)
    }

    fun updateBaseUrl(value: String) {
        _uiState.value = _uiState.value.copy(baseUrl = value, infoMessage = null, errorMessage = null)
    }

    fun saveBaseUrl() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, infoMessage = null, errorMessage = null)
            when (val result = authRepository.updateBaseUrl(_uiState.value.baseUrl)) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(isSaving = false, infoMessage = "Backend URL saved.")
                is Resource.Error -> _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = result.message)
                Resource.Loading -> Unit
            }
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onLoggedOut()
        }
    }
}
