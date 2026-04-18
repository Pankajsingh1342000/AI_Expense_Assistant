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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.epic.aiexpensevoice.domain.model.ExpenseItem
import com.epic.aiexpensevoice.presentation.components.ArchitectTopBar
import com.epic.aiexpensevoice.presentation.components.EmptyStateCard
import com.epic.aiexpensevoice.presentation.components.ExpenseRowCard
import com.epic.aiexpensevoice.presentation.components.HeroMetricCard
import com.epic.aiexpensevoice.presentation.components.SectionCard
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(viewModel: ExpensesViewModel) {
    val state by viewModel.uiState.collectAsState()
    var editingExpense by remember { mutableStateOf<ExpenseItem?>(null) }
    var deletingExpense by remember { mutableStateOf<ExpenseItem?>(null) }

    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item { ArchitectTopBar(title = "Expenses") }
            item {
                com.epic.aiexpensevoice.presentation.components.SectionCard {
                    androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                        androidx.compose.material3.Text("Expense Entries", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                        androidx.compose.material3.Text(state.expenses.size.toString(), style = androidx.compose.material3.MaterialTheme.typography.displayLarge)
                        androidx.compose.material3.Text("Review and manage your latest spending activity", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            state.infoMessage?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
            state.errorMessage?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            if (state.isLoading && state.expenses.isEmpty()) {
                item { EmptyStateCard("Loading expenses", "Bringing your entries into view.") }
            } else if (state.expenses.isEmpty()) {
                item {
                    EmptyStateCard(
                        "No expenses yet",
                        "When you add an expense via voice or chat, it will appear here.",
                        actionLabel = "Refresh",
                        onAction = viewModel::refresh,
                    )
                }
            } else {
                items(state.expenses) { expense ->
                    ExpenseRowCard(
                        expense = expense,
                        trailing = {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(onClick = { editingExpense = expense }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                                IconButton(onClick = { deletingExpense = expense }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        },
                    )
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
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deletingExpense = null }) { Text("Cancel") }
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
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update expense") },
        text = {
            SectionCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(expense.title, style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { 
                            amountText = it
                            isError = false
                        },
                        label = { Text("Amount") },
                        singleLine = true,
                        isError = isError,
                        shape = RoundedCornerShape(999.dp),
                        supportingText = if (isError) { { Text("Please enter a valid number") } } else null
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { 
                    val parsed = amountText.toDoubleOrNull()
                    if (parsed != null && parsed > 0) {
                        onSave(parsed)
                    } else {
                        isError = true
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
