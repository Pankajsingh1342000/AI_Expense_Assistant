package com.epic.aiexpensevoice.presentation.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epic.aiexpensevoice.core.common.Resource
import com.epic.aiexpensevoice.data.repository.AuthRepository
import com.epic.aiexpensevoice.presentation.state.AuthUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun updateName(value: String) {
        _uiState.value = _uiState.value.copy(name = value, errorMessage = null)
    }

    fun updateEmail(value: String) {
        _uiState.value = _uiState.value.copy(email = value, errorMessage = null)
    }

    fun updatePassword(value: String) {
        _uiState.value = _uiState.value.copy(password = value, errorMessage = null)
    }

    fun updateConfirmPassword(value: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = value, errorMessage = null)
    }

    fun login(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Email and password are required.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.login(state.email.trim(), state.password)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                }
                is Resource.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                Resource.Loading -> Unit
            }
        }
    }

    fun register(onSuccess: () -> Unit) {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Email and password are required.")
            return
        }
        if (state.password != state.confirmPassword) {
            _uiState.value = state.copy(errorMessage = "Passwords do not match.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            when (val result = authRepository.register(state.email.trim(), state.password)) {
                is Resource.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                }
                is Resource.Error -> _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = result.message)
                Resource.Loading -> Unit
            }
        }
    }
}
