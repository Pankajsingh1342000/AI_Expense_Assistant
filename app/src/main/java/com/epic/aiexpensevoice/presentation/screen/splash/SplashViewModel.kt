package com.epic.aiexpensevoice.presentation.screen.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epic.aiexpensevoice.data.local.UserSession
import com.epic.aiexpensevoice.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SplashViewModel(authRepository: AuthRepository) : ViewModel() {
    private val _session = MutableStateFlow<UserSession?>(null)
    val session: StateFlow<UserSession?> = _session.asStateFlow()

    init {
        authRepository.session
            .onEach { _session.value = it }
            .launchIn(viewModelScope)
    }
}
