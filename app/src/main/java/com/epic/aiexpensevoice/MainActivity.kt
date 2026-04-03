package com.epic.aiexpensevoice

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.epic.aiexpensevoice.core.designsystem.theme.AIExpenseVoiceTheme
import com.epic.aiexpensevoice.presentation.navigation.AppNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as AiExpenseVoiceApplication).container
        setContent {
            AIExpenseVoiceTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph(container)
                }
            }
        }
    }
}
