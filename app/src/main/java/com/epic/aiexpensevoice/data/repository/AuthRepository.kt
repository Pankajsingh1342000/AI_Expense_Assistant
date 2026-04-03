package com.epic.aiexpensevoice.data.repository

import com.epic.aiexpensevoice.core.common.Resource
import com.epic.aiexpensevoice.data.local.SessionManager
import com.epic.aiexpensevoice.data.local.chat.ChatHistoryStore
import com.epic.aiexpensevoice.data.local.db.DashboardCacheStore
import com.epic.aiexpensevoice.data.local.db.LocalExpenseStore
import com.epic.aiexpensevoice.data.remote.dto.RegisterRequestDto
import com.epic.aiexpensevoice.data.remote.network.ApiService

class AuthRepository(
    private val apiService: ApiService,
    private val sessionManager: SessionManager,
    private val dashboardCacheStore: DashboardCacheStore,
    private val localExpenseStore: LocalExpenseStore,
    private val chatHistoryStore: ChatHistoryStore,
) {
    val session = sessionManager.session

    suspend fun register(email: String, password: String): Resource<Unit> = runCatching {
        val response = apiService.register(RegisterRequestDto(email = email, password = password))
        if (!response.isSuccessful) error("Registration failed with ${response.code()}")
    }.fold(
        onSuccess = { Resource.Success(Unit) },
        onFailure = { Resource.Error(it.message ?: "Unable to register.", it) },
    )

    suspend fun login(email: String, password: String): Resource<Unit> = runCatching {
        val response = apiService.login(email = email, password = password)
        val body = response.body()
        if (!response.isSuccessful || body == null) error("Login failed with ${response.code()}")
        sessionManager.saveAuth(
            accessToken = body.access_token,
            tokenType = body.token_type,
            email = email,
            name = email.substringBefore("@"),
        )
    }.fold(
        onSuccess = { Resource.Success(Unit) },
        onFailure = { Resource.Error(it.message ?: "Unable to login.", it) },
    )

    suspend fun updateBaseUrl(baseUrl: String): Resource<Unit> = runCatching {
        sessionManager.updateBaseUrl(baseUrl)
    }.fold(
        onSuccess = { Resource.Success(Unit) },
        onFailure = { Resource.Error(it.message ?: "Could not update the base URL.", it) },
    )

    suspend fun logout() {
        dashboardCacheStore.clear()
        localExpenseStore.clearAll()
        chatHistoryStore.clear()
        sessionManager.clearSession()
    }
}
