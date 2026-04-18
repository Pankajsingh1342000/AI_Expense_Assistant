package com.epic.aiexpensevoice.presentation.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.epic.aiexpensevoice.core.common.asCurrency
import com.epic.aiexpensevoice.core.common.toDisplayDateLabel
import com.epic.aiexpensevoice.domain.model.BudgetStatus
import com.epic.aiexpensevoice.domain.model.BudgetTone
import com.epic.aiexpensevoice.domain.model.CategorySpend
import com.epic.aiexpensevoice.domain.model.ExpenseItem
import com.epic.aiexpensevoice.core.designsystem.theme.PastelGreen
import com.epic.aiexpensevoice.core.designsystem.theme.PastelOrange
import com.epic.aiexpensevoice.core.designsystem.theme.PastelPurple
import com.epic.aiexpensevoice.domain.model.TrendPoint

@Composable
fun GradientScreen(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
    ) {
        content()
    }
}

@Composable
fun ArchitectTopBar(
    title: String = "David",
    trailingContent: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Hello,",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (trailingContent != null) {
            trailingContent()
        } else {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.size(52.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HeroMetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            subtitle?.let {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.padding(24.dp)) { content() }
    }
}

@Composable
fun SuggestionChips(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(suggestions) { suggestion ->
            AssistChip(
                onClick = { onSuggestionClick(suggestion) },
                label = {
                    Text(
                        text = suggestion,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                shape = RoundedCornerShape(999.dp),
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onSurface,
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}

@Composable
fun VoiceInputButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Mic,
    isListening: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = 0.dp,
        border = null,
        modifier = modifier.size(54.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isListening) {
                CircularProgressIndicator(
                    modifier = Modifier.size(42.dp),
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.28f),
                )
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
fun BudgetProgressCard(status: BudgetStatus) {
    val toneColor = when (status.tone) {
        BudgetTone.Healthy -> MaterialTheme.colorScheme.secondary
        BudgetTone.Watch -> MaterialTheme.colorScheme.tertiary
        BudgetTone.Risk -> MaterialTheme.colorScheme.error
    }
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(status.category, style = MaterialTheme.typography.titleMedium)
                Text(status.statusLabel, style = MaterialTheme.typography.labelMedium, color = toneColor)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(toneColor.copy(alpha = 0.15f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(status.progress.coerceIn(0f, 1f))
                        .height(7.dp)
                        .clip(CircleShape)
                        .background(toneColor),
                )
            }
            Text(
                "${status.spent.asCurrency()} of ${status.limit.asCurrency()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun CategoryPieChartCard(items: List<CategorySpend>) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Category share", style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Canvas(modifier = Modifier.size(150.dp)) {
                    var startAngle = -90f
                    items.forEach { item ->
                        val sweep = item.share * 360f
                        drawArc(
                            color = item.color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = 26.dp.toPx(), cap = StrokeCap.Round),
                            size = Size(size.width, size.height),
                        )
                        startAngle += sweep
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items.take(4).forEach { item ->
                        Row(verticalAlignment = Alignment.Top) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(item.color),
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(item.category, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${(item.share * 100).toInt()}% - ${item.amount.asCurrency()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrendChartCard(points: List<TrendPoint>) {
    if (points.count { it.amount > 0 } < 2) {
        SectionCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Spending trend", style = MaterialTheme.typography.titleLarge)
                Text(
                    "We need a few more days of activity before a trend chart becomes useful.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        return
    }

    val activeBarColor = MaterialTheme.colorScheme.primaryContainer
    val idleBarColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val maxPoint = points.maxByOrNull { it.amount }
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Spending trend", style = MaterialTheme.typography.titleLarge)
                Text("Last 7 days", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            val maxAmount = points.maxOfOrNull { it.amount }?.toFloat()?.coerceAtLeast(1f) ?: 1f
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                val maxBarWidth = 100f
                val barWidth = (size.width / (points.size * 1.6f).coerceAtLeast(1f)).coerceAtMost(maxBarWidth)
                val step = if (points.isEmpty()) 0f else size.width / points.size
                points.forEachIndexed { index, point ->
                    val left = (step * index) + ((step - barWidth) / 2f)
                    val barHeight = (point.amount.toFloat() / maxAmount) * (size.height * 0.9f)
                    val topLeft = Offset(left, size.height - barHeight)
                    drawRoundRect(
                        color = if (point == maxPoint) activeBarColor else idleBarColor,
                        topLeft = topLeft,
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f),
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                points.forEach {
                    Text(it.label.toDisplayDateLabel(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    SectionCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                FilledTonalButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(999.dp),
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
fun ExpenseRowCard(
    expense: ExpenseItem,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val bgColor = when (expense.category.lowercase()) {
                "food" -> PastelOrange
                "salary" -> PastelGreen
                "entertainment" -> PastelPurple
                else -> PastelOrange
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = bgColor,
                modifier = Modifier.size(54.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        expense.title.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = com.epic.aiexpensevoice.core.designsystem.theme.ArchitectText,
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(expense.title, style = MaterialTheme.typography.titleMedium)
                Text(expense.category, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val isIncome = expense.amount >= 0
            Text(
                (if (isIncome) "" else "-") + expense.amount.asCurrency(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                expense.dateLabel.toDisplayDateLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            trailing?.invoke()
        }
    }
}

@Composable
fun GradientPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium)
    }
}
