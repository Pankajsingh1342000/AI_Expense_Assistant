package com.epic.aiexpensevoice.presentation.screen.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.epic.aiexpensevoice.presentation.components.GradientScreen

@Composable
fun SplashScreen() {
    GradientScreen {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("AI Expense Voice", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Voice-first budgeting and conversational expense tracking.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
            )
            CircularProgressIndicator(modifier = Modifier.padding(top = 28.dp))
        }
    }
}
