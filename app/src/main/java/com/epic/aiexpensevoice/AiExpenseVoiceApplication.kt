package com.epic.aiexpensevoice

import android.app.Application
import com.epic.aiexpensevoice.core.common.AppContainer

class AiExpenseVoiceApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
