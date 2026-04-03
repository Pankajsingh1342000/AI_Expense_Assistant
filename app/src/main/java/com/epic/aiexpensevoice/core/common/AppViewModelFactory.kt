package com.epic.aiexpensevoice.core.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.epic.aiexpensevoice.presentation.screen.auth.AuthViewModel
import com.epic.aiexpensevoice.presentation.screen.chat.ChatViewModel
import com.epic.aiexpensevoice.presentation.screen.dashboard.DashboardViewModel
import com.epic.aiexpensevoice.presentation.screen.expenses.ExpensesViewModel
import com.epic.aiexpensevoice.presentation.screen.profile.ProfileViewModel
import com.epic.aiexpensevoice.presentation.screen.splash.SplashViewModel

class AppViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when {
            modelClass.isAssignableFrom(SplashViewModel::class.java) -> SplashViewModel(container.authRepository) as T
            modelClass.isAssignableFrom(AuthViewModel::class.java) -> AuthViewModel(container.authRepository) as T
            modelClass.isAssignableFrom(ChatViewModel::class.java) -> ChatViewModel(container.agentRepository, container.chatHistory) as T
            modelClass.isAssignableFrom(DashboardViewModel::class.java) -> DashboardViewModel(container.analyticsRepository) as T
            modelClass.isAssignableFrom(ExpensesViewModel::class.java) -> ExpensesViewModel(container.expenseRepository) as T
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> ProfileViewModel(container.authRepository) as T
            else -> error("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
