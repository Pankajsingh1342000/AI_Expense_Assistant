package com.epic.aiexpensevoice.presentation.screen.expenses

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.epic.aiexpensevoice.core.common.asCurrency
import com.epic.aiexpensevoice.domain.model.ExpenseItem
import com.epic.aiexpensevoice.presentation.components.EmptyStateCard

@Composable
fun ExpensesScreen(viewModel: ExpensesViewModel) {
    val state by viewModel.uiState.collectAsState()
    var editingExpense by remember { mutableStateOf<ExpenseItem?>(null) }
    var deletingExpense by remember { mutableStateOf<ExpenseItem?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Expenses", style = MaterialTheme.typography.headlineMedium)
                    Text("Review recent entries, make quick edits, or remove anything that looks off.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
                    Button(onClick = viewModel::refresh) {
                        Text("Refresh")
                    }
                }
            }
        }
        state.infoMessage?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.primary) }
        }
        state.errorMessage?.let { message ->
            item { Text(message, color = MaterialTheme.colorScheme.error) }
        }
        if (state.isLoading && state.expenses.isEmpty()) {
            item { EmptyStateCard("Loading expenses", "Bringing your entries into view.") }
        }
        if (!state.isLoading && state.expenses.isEmpty()) {
            item { EmptyStateCard("No expenses yet", "Add your first expense in chat and it will show up here automatically.") }
        }
        items(state.expenses) { expense ->
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                        Text(expense.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        Text(expense.amount.asCurrency(), style = MaterialTheme.typography.titleLarge)
                        Text("${expense.category} - ${expense.dateLabel}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                    Row {
                        IconButton(onClick = {
                            viewModel.clearMessages()
                            editingExpense = expense
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = {
                            viewModel.clearMessages()
                            deletingExpense = expense
                        }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete")
                        }
                    }
                }
            }
        }
    }

    editingExpense?.let { expense ->
        EditExpenseDialog(
            expense = expense,
            onDismiss = { editingExpense = null },
            onSave = { amount ->
                viewModel.updateExpense(expense, amount)
                editingExpense = null
            },
        )
    }

    deletingExpense?.let { expense ->
        AlertDialog(
            onDismissRequest = { deletingExpense = null },
            title = { Text("Delete expense") },
            text = { Text("Remove ${expense.title} from your expense list?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteExpense(expense)
                    deletingExpense = null
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingExpense = null }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun EditExpenseDialog(
    expense: ExpenseItem,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
) {
    var amountText by remember(expense.title) { mutableStateOf(expense.amount.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update expense") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(expense.title)
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount") },
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    amountText.toDoubleOrNull()?.let(onSave)
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
