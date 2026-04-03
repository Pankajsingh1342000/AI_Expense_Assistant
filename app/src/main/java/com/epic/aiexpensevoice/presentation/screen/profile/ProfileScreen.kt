package com.epic.aiexpensevoice.presentation.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.epic.aiexpensevoice.presentation.components.EmptyStateCard

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLoggedOut: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Account", style = MaterialTheme.typography.headlineMedium)
                    Text(state.email.ifBlank { "No email available" }, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
                    Text("Your assistant stays in sync across chat, dashboard, and expenses.", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Connection settings", style = MaterialTheme.typography.titleLarge)
                    Text("Only change this if you're switching to another backend environment.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    OutlinedTextField(
                        value = state.baseUrl,
                        onValueChange = viewModel::updateBaseUrl,
                        label = { Text("Base URL") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = viewModel::saveBaseUrl, modifier = Modifier.fillMaxWidth(), enabled = !state.isSaving) {
                        Text(if (state.isSaving) "Saving..." else "Save changes")
                    }
                    state.infoMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                    state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
        item {
            EmptyStateCard("Voice tips", "Tap the mic in chat, speak naturally, then pause. You can say things like 'Add lunch 220' or 'Any budget warning?'.")
        }
        item {
            Button(onClick = { viewModel.logout(onLoggedOut) }, modifier = Modifier.fillMaxWidth()) {
                Text("Sign out")
            }
        }
    }
}
