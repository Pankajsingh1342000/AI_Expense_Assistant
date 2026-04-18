package com.epic.aiexpensevoice.presentation.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.epic.aiexpensevoice.core.common.asCurrency
import com.epic.aiexpensevoice.presentation.components.ArchitectTopBar
import com.epic.aiexpensevoice.presentation.components.BudgetProgressCard
import com.epic.aiexpensevoice.presentation.components.CategoryPieChartCard
import com.epic.aiexpensevoice.presentation.components.EmptyStateCard
import com.epic.aiexpensevoice.presentation.components.ExpenseRowCard
import com.epic.aiexpensevoice.presentation.components.HeroMetricCard
import com.epic.aiexpensevoice.presentation.components.SectionCard
import com.epic.aiexpensevoice.presentation.components.TrendChartCard

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val state by viewModel.uiState.collectAsState()
    val data = state.data

    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item { ArchitectTopBar(title = "Overview") }
            if (state.isLoading && data == null) {
                item {
                    EmptyStateCard("Getting things ready", "Pulling together your spending, budgets, and trends.")
                }
            }
            data?.let { dashboard ->
                item {
                    HeroMetricCard(
                        title = "Total Spent This Month",
                        value = dashboard.totalSpent.asCurrency(),
                        subtitle = if (dashboard.isEmpty) null else dashboard.totalChangeLabel,
                    )
                }
                if (dashboard.isEmpty) {
                    item {
                        EmptyStateCard(
                            "Your dashboard is ready",
                            "Add your first expense and this space will turn into a calm spending overview with trends, categories, and budget progress.",
                            actionLabel = "Refresh",
                            onAction = viewModel::refresh,
                        )
                    }
                } else {
                    dashboard.topCategory?.let { top ->
                        item {
                            SectionCard {
                                androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("TOP CATEGORY", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                                    Text(top.category, style = MaterialTheme.typography.titleLarge)
                                    Text(top.amount.asCurrency(), style = MaterialTheme.typography.headlineMedium)
                                    androidx.compose.foundation.layout.Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                shape = androidx.compose.foundation.shape.CircleShape,
                                            ),
                                    ) {
                                        androidx.compose.foundation.layout.Box(
                                            modifier = Modifier
                                                .fillMaxWidth(top.share.coerceIn(0.08f, 1f))
                                                .padding(vertical = 2.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    shape = androidx.compose.foundation.shape.CircleShape,
                                                ),
                                        )
                                    }
                                    Text(
                                        "${(top.share * 100).toInt()}% of current category mix",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                    if (dashboard.categories.isNotEmpty()) {
                        item { CategoryPieChartCard(dashboard.categories) }
                    }
                    if (dashboard.trendPoints.isNotEmpty()) {
                        item {
                            TrendChartCard(dashboard.trendPoints)
                        }
                    }
                    if (dashboard.budgets.isNotEmpty()) {
                        item { Text("Budget Overview", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        items(dashboard.budgets) { budget -> BudgetProgressCard(budget) }
                    }
                    if (dashboard.recentExpenses.isNotEmpty()) {
                        item { Text("Recent Expenses", style = MaterialTheme.typography.titleLarge) }
                        items(dashboard.recentExpenses.take(5)) { expense ->
                            ExpenseRowCard(expense = expense)
                        }
                    }
                    if (dashboard.insight.isNotBlank() && dashboard.insight != "No insight available yet.") {
                        item {
                            SectionCard {
                                androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Spending insight", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                    Text(dashboard.insight, style = MaterialTheme.typography.bodyLarge)
                                    Text(dashboard.summaryLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            state.errorMessage?.let { message ->
                item { EmptyStateCard("Couldn't refresh right now", message, actionLabel = "Try again", onAction = viewModel::refresh) }
            }
        }
    }
}
