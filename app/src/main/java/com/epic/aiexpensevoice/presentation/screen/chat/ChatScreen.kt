package com.epic.aiexpensevoice.presentation.screen.chat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.epic.aiexpensevoice.core.common.Constants
import com.epic.aiexpensevoice.core.common.asCurrency
import com.epic.aiexpensevoice.domain.model.AgentContentBlock
import com.epic.aiexpensevoice.domain.model.ChatMessageUiModel
import com.epic.aiexpensevoice.domain.model.Sender
import com.epic.aiexpensevoice.presentation.components.BudgetProgressCard
import com.epic.aiexpensevoice.presentation.components.EmptyStateCard
import com.epic.aiexpensevoice.presentation.components.SuggestionChips
import com.epic.aiexpensevoice.presentation.components.TrendChartCard
import com.epic.aiexpensevoice.presentation.components.VoiceInputButton

private val prompts = listOf(
    "Add coffee 120",
    "Show my expenses",
    "Monthly summary",
    "Set food budget 5000",
    "Any budget warning?",
    "Spending trend",
)

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.setListening(false)
        if (result.resultCode == Activity.RESULT_OK) {
            val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty()
            if (text.isNotBlank()) {
                viewModel.onTranscriptCaptured(text)
            } else {
                viewModel.onVoiceInputEmpty()
            }
        } else {
            viewModel.onVoiceInputEmpty()
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.updateVoicePermission(granted)
        if (granted) {
            viewModel.setListening(true)
            speechLauncher.launch(speechIntent())
        } else {
            viewModel.onVoicePermissionDenied()
        }
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        viewModel.updateVoicePermission(granted)
    }

    LaunchedEffect(state.messages.size, state.isSending) {
        val extraItems = if (state.isSending) 1 else 0
        val targetIndex = (state.messages.size + extraItems - 1).coerceAtLeast(0)
        if (targetIndex >= 0) {
            listState.animateScrollToItem(targetIndex)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        ChatHeader()
        SuggestionChips(prompts) { viewModel.sendMessage(it) }
        Spacer(Modifier.height(12.dp))
        if (state.messages.size <= 1) {
            EmptyStateCard(
                "Your money assistant is ready",
                "Ask a question, log an expense, or tap the mic and speak naturally. Start simple and the app will guide you from there.",
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            items(state.messages) { message -> MessageBubble(message = message, onSuggestionClick = viewModel::sendMessage) }
            if (state.isSending) {
                item {
                    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Text("Working on it...", modifier = Modifier.padding(18.dp))
                    }
                }
            }
        }
        ChatStatusLine(
            message = state.errorMessage ?: state.infoMessage ?: if (state.isListening) "Listening..." else null,
            isError = state.errorMessage != null,
        )
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 16.dp)) {
            VoiceInputButton(
                isListening = state.isListening,
                enabled = true,
                onClick = {
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (granted) {
                        viewModel.setListening(true)
                        speechLauncher.launch(speechIntent())
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
            )
            OutlinedTextField(
                value = state.draft,
                onValueChange = viewModel::updateDraft,
                placeholder = { Text("Ask about spending or say Add coffee 120") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
            )
            Surface(modifier = Modifier.size(54.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondary) {
                IconButton(onClick = { viewModel.sendMessage() }) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun ChatHeader() {
    Card(shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)),
                    ),
                )
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("AI Expense Assistant", style = MaterialTheme.typography.titleLarge)
                Text("Track spending, check budgets, and get quick answers in one conversation.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessageUiModel,
    onSuggestionClick: (String) -> Unit,
) {
    val isUser = message.sender == Sender.User
    Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start, modifier = Modifier.fillMaxWidth()) {
        Card(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = if (isUser) 24.dp else 6.dp, bottomEnd = if (isUser) 6.dp else 24.dp),
            colors = CardDefaults.cardColors(containerColor = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(if (isUser) 0.86f else 0.96f),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(message.text, color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                if (!isUser) {
                    message.blocks.forEach { block ->
                        when (block) {
                            is AgentContentBlock.PlainText -> Text(block.value, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f))
                            is AgentContentBlock.Warning -> Text("${block.title}: ${block.detail}", color = MaterialTheme.colorScheme.error)
                            is AgentContentBlock.Clarification -> SuggestionChips(block.options, onSuggestionClick)
                            is AgentContentBlock.ExpenseList -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                block.expenses.forEach { expense ->
                                    Card(shape = RoundedCornerShape(18.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Column {
                                                Text(expense.title)
                                                Text("${expense.category} - ${expense.dateLabel}", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                            }
                                            Text(expense.amount.asCurrency())
                                        }
                                    }
                                }
                            }
                            is AgentContentBlock.BudgetOverview -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                block.budgets.forEach { BudgetProgressCard(it) }
                            }
                            is AgentContentBlock.CategoryBreakdown -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                block.items.take(4).forEach { category ->
                                    Text("${category.category}: ${category.amount.asCurrency()}")
                                }
                            }
                            is AgentContentBlock.TrendChart -> TrendChartCard(block.points)
                            is AgentContentBlock.Success -> Text("${block.title}: ${block.detail}")
                            is AgentContentBlock.TotalsSummary -> Text("${block.title}: ${block.amount.asCurrency()} - ${block.subtitle}")
                            is AgentContentBlock.Insight -> Text(block.detail)
                        }
                    }
                }
            }
        }
        Text(message.timestampLabel, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.padding(8.dp))
    }
}

@Composable
private fun ChatStatusLine(
    message: String?,
    isError: Boolean,
) {
    if (message.isNullOrBlank()) return
    Text(
        text = message,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

private fun speechIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Constants.VoiceLocale)
    putExtra(RecognizerIntent.EXTRA_PROMPT, "Say an expense or ask your assistant")
}
