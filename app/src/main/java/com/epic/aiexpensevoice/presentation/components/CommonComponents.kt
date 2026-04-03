package com.epic.aiexpensevoice.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.epic.aiexpensevoice.core.common.asCurrency
import com.epic.aiexpensevoice.domain.model.BudgetStatus
import com.epic.aiexpensevoice.domain.model.BudgetTone
import com.epic.aiexpensevoice.domain.model.CategorySpend
import com.epic.aiexpensevoice.domain.model.TrendPoint

@Composable
fun GradientScreen(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface),
                ),
            ),
    ) { content() }
}

@Composable
fun SuggestionChips(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(suggestions) { suggestion ->
            AssistChip(
                onClick = { onSuggestionClick(suggestion) },
                label = { Text(suggestion) },
                colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
            )
        }
    }
}

@Composable
fun VoiceInputButton(
    isListening: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        tonalElevation = 6.dp,
        color = if (isListening) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(78.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isListening) {
                CircularProgressIndicator(
                    modifier = Modifier.size(56.dp),
                    strokeWidth = 4.dp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.4f),
                )
            }
            Icon(Icons.Default.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
fun BudgetProgressCard(status: BudgetStatus) {
    val toneColor = when (status.tone) {
        BudgetTone.Healthy -> MaterialTheme.colorScheme.primary
        BudgetTone.Watch -> MaterialTheme.colorScheme.tertiary
        BudgetTone.Risk -> MaterialTheme.colorScheme.error
    }
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(status.category, style = MaterialTheme.typography.titleMedium)
                Text(status.statusLabel, style = MaterialTheme.typography.labelLarge, color = toneColor)
            }
            LinearProgressIndicator(
                progress = { status.progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                color = toneColor,
                trackColor = toneColor.copy(alpha = 0.18f),
            )
            Text("${status.spent.asCurrency()} of ${status.limit.asCurrency()}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun CategoryPieChartCard(items: List<CategorySpend>) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Category share", style = MaterialTheme.typography.titleLarge)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Canvas(modifier = Modifier.size(140.dp)) {
                    var startAngle = -90f
                    items.forEach { item ->
                        val sweep = item.share * 360f
                        drawArc(
                            color = item.color,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            style = Stroke(width = 28.dp.toPx(), cap = StrokeCap.Round),
                            size = Size(size.width, size.height),
                        )
                        startAngle += sweep
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items.take(4).forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(item.color))
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(item.category, style = MaterialTheme.typography.labelLarge)
                                Text("${(item.share * 100).toInt()}% • ${item.amount.asCurrency()}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
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
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("Spending trend", style = MaterialTheme.typography.titleLarge)
            val maxAmount = points.maxOfOrNull { it.amount }?.toFloat()?.coerceAtLeast(1f) ?: 1f
            Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                val count = points.size.coerceAtLeast(2)
                val segment = if (count == 1) size.width else size.width / (count - 1)
                val linePoints = points.mapIndexed { index, point ->
                    val x = segment * index
                    val y = size.height - ((point.amount.toFloat() / maxAmount) * (size.height * 0.8f)) - 10f
                    Offset(x, y)
                }
                linePoints.zipWithNext().forEach { (start, end) ->
                    drawLine(primary, start = start, end = end, strokeWidth = 8f, cap = StrokeCap.Round)
                }
                linePoints.forEach { point ->
                    drawCircle(secondary, radius = 9f, center = point)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                points.forEach { point -> Text(point.label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)) }
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
    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(body, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                Button(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}
