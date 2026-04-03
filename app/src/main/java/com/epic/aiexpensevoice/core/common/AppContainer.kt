package com.epic.aiexpensevoice.core.common

import android.content.Context
import com.epic.aiexpensevoice.data.local.chat.ChatHistoryStore
import com.epic.aiexpensevoice.data.local.db.AppDatabase
import com.epic.aiexpensevoice.data.local.db.DashboardCacheStore
import com.epic.aiexpensevoice.data.local.db.LocalExpenseStore
import com.epic.aiexpensevoice.data.local.SessionManager
import com.epic.aiexpensevoice.data.mapper.AgentResponseParser
import com.epic.aiexpensevoice.data.remote.network.BaseUrlHolder
import com.epic.aiexpensevoice.data.remote.network.NetworkModule
import com.epic.aiexpensevoice.data.repository.AgentRepository
import com.epic.aiexpensevoice.data.repository.AnalyticsRepository
import com.epic.aiexpensevoice.data.repository.AuthRepository
import com.epic.aiexpensevoice.data.repository.ExpenseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class AppContainer(context: Context) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val sessionManager = SessionManager(context.applicationContext)
    private val chatHistoryStore = ChatHistoryStore(context.applicationContext)
    private val database = AppDatabase.create(context.applicationContext)
    private val dashboardCacheStore = DashboardCacheStore(database.dashboardCacheDao())
    private val localExpenseStore = LocalExpenseStore(
        expenseCacheDao = database.expenseCacheDao(),
        pendingSyncDao = database.pendingSyncDao(),
    )

    private val baseUrlHolder = BaseUrlHolder(Constants.PlaceholderBaseUrl)
    private val parser = AgentResponseParser()
    private val apiService = NetworkModule.createApiService(
        context = context.applicationContext,
        sessionManager = sessionManager,
        baseUrlHolder = baseUrlHolder,
    )

    val authRepository = AuthRepository(
        apiService = apiService,
        sessionManager = sessionManager,
        dashboardCacheStore = dashboardCacheStore,
        localExpenseStore = localExpenseStore,
        chatHistoryStore = chatHistoryStore,
    )
    val agentRepository = AgentRepository(apiService, parser, localExpenseStore)
    val expenseRepository = ExpenseRepository(apiService, localExpenseStore)
    val analyticsRepository = AnalyticsRepository(apiService, dashboardCacheStore, localExpenseStore)
    val chatHistory = chatHistoryStore

    init {
        sessionManager.session
            .onEach { baseUrlHolder.baseUrl = it.resolvedBaseUrl }
            .launchIn(appScope)
    }
}
