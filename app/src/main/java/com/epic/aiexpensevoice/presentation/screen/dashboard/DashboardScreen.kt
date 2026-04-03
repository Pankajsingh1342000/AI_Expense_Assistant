package com.epic.aiexpensevoice.presentation.screen.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.epic.aiexpensevoice.core.common.asCurrency
import com.epic.aiexpensevoice.presentation.components.BudgetProgressCard
import com.epic.aiexpensevoice.presentation.components.CategoryPieChartCard
import com.epic.aiexpensevoice.presentation.components.EmptyStateCard
import com.epic.aiexpensevoice.presentation.components.TrendChartCard

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val state by viewModel.uiState.collectAsState()
    val data = state.data

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            if (state.isLoading && data == null) {
                EmptyStateCard("Getting things ready", "Pulling together your spending, budgets, and trends.")
            }
        }
        data?.let {
            item {
                Card(shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("This month", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text(data.totalSpent.asCurrency(), style = MaterialTheme.typography.headlineLarge)
                        if (!data.isEmpty) {
                            Text(data.totalChangeLabel, color = MaterialTheme.colorScheme.primary)
                        }
                        data.topCategory?.let { top ->
                            Text("Top category: ${top.category} - ${top.amount.asCurrency()}", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            if (data.isEmpty) {
                item {
                    EmptyStateCard(
                        "No activity yet",
                        "Start with a quick chat entry like 'Add coffee 120' or set a budget to begin seeing your spending story here.",
                    )
                }
            } else {
                if (data.categories.isNotEmpty()) {
                    item { CategoryPieChartCard(data.categories) }
                }
                if (data.trendPoints.isNotEmpty()) {
                    item { TrendChartCard(data.trendPoints) }
                }
                if (data.insight != "No insight available yet.") {
                    item {
                        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("AI insight", style = MaterialTheme.typography.titleLarge)
                                Text(data.insight, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                Text(data.summaryLabel, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                if (data.budgets.isNotEmpty()) {
                    item { Text("Budget overview", style = MaterialTheme.typography.titleLarge) }
                    items(data.budgets) { budget ->
                        BudgetProgressCard(budget)
                    }
                }
                if (data.recentExpenses.isNotEmpty()) {
                    item { Text("Recent expenses", style = MaterialTheme.typography.titleLarge) }
                    items(data.recentExpenses) { expense ->
                        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(expense.title, fontWeight = FontWeight.SemiBold)
                                    Text("${expense.category} - ${expense.dateLabel}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                }
                                Text(expense.amount.asCurrency())
                            }
                        }
                    }
                }
            }
        }
        state.errorMessage?.let { message ->
            item { EmptyStateCard("Couldn't refresh right now", body = message, actionLabel = "Try again", onAction = viewModel::refresh) }
        }
        if (data != null) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = viewModel::refresh) {
                        Text("Refresh")
                    }
                }
            }
        }
    }
}
