package com.epic.aiexpensevoice.presentation.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.epic.aiexpensevoice.presentation.components.ArchitectTopBar
import com.epic.aiexpensevoice.presentation.components.EmptyStateCard
import com.epic.aiexpensevoice.presentation.components.GradientPrimaryButton
import com.epic.aiexpensevoice.presentation.components.SectionCard

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onLoggedOut: () -> Unit,
) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(contentPadding = PaddingValues(bottom = 132.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { ArchitectTopBar(title = "Profile") }
        item {
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Account", style = MaterialTheme.typography.titleLarge)
                    Text(state.email.ifBlank { "No email available" }, style = MaterialTheme.typography.bodyLarge)
                    Text("Your assistant stays in sync across chat, dashboard, and expenses.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Connection Settings", style = MaterialTheme.typography.titleLarge)
                    Text("Only change this when switching environments.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = state.baseUrl,
                        onValueChange = viewModel::updateBaseUrl,
                        label = { Text("Base URL") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                    )
                    GradientPrimaryButton(
                        text = if (state.isSaving) "Saving..." else "Save Changes",
                        onClick = viewModel::saveBaseUrl,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isSaving,
                    )
                    state.infoMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                    state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
        item {
            EmptyStateCard("Voice tips", "Tap the mic in chat and say things like 'Add lunch 220' or 'Any budget warning?'.")
        }
        item {
            GradientPrimaryButton(
                text = "Sign Out",
                onClick = { viewModel.logout(onLoggedOut) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )
        }
    }
}
