package com.epic.aiexpensevoice.presentation.screen.chat

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.epic.aiexpensevoice.core.common.Constants
import com.epic.aiexpensevoice.core.common.asCurrency
import com.epic.aiexpensevoice.core.common.toDisplayDateLabel
import com.epic.aiexpensevoice.core.designsystem.theme.AIExpenseVoiceTheme
import com.epic.aiexpensevoice.domain.model.AgentContentBlock
import com.epic.aiexpensevoice.domain.model.ChatMessageUiModel
import com.epic.aiexpensevoice.domain.model.Sender
import com.epic.aiexpensevoice.presentation.components.ArchitectTopBar
import com.epic.aiexpensevoice.presentation.components.BudgetProgressCard
import com.epic.aiexpensevoice.presentation.components.EmptyStateCard
import com.epic.aiexpensevoice.presentation.components.SectionCard
import com.epic.aiexpensevoice.presentation.components.SuggestionChips
import com.epic.aiexpensevoice.presentation.components.TrendChartCard
import com.epic.aiexpensevoice.presentation.components.VoiceInputButton
import com.epic.aiexpensevoice.presentation.state.ChatUiState

private val prompts = listOf(
    "Add pizza 250 in food",
    "Show monthly summary",
    "What are my expenses?",
)

@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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

    ChatScreenContent(
        state = state,
        onSendMessage = viewModel::sendMessage,
        onUpdateDraft = viewModel::updateDraft,
        onVoiceInputClick = {
            val isDraftEmpty = state.draft.isBlank()
            if (isDraftEmpty) {
                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (granted) {
                    viewModel.setListening(true)
                    speechLauncher.launch(speechIntent())
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            } else {
                viewModel.sendMessage()
            }
        }
    )
}

@Composable
fun ChatScreenContent(
    state: ChatUiState,
    onSendMessage: (String) -> Unit,
    onUpdateDraft: (String) -> Unit,
    onVoiceInputClick: () -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.messages.size, state.isSending) {
        val extraItems = if (state.isSending) 1 else 0
        val targetIndex = (state.messages.size + extraItems - 1).coerceAtLeast(0)
        listState.animateScrollToItem(targetIndex)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ArchitectTopBar(title = "AI Assistant")
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        "Hi! I'm your financial copilot. Ask me anything about your spending.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (state.messages.size <= 1) {
                item {
                    EmptyStateCard(
                        "Start with a quick question",
                        "Try asking for your monthly summary, latest expenses, or tell me to add a new expense in plain language.",
                    )
                }
            }
            items(state.messages) { message ->
                MessageBubble(
                    message = message,
                    onSuggestionClick = onSendMessage,
                )
            }
            if (state.isSending) {
                item {
                    SectionCard {
                        Text("Working on it...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        SuggestionChips(prompts) { onSendMessage(it) }
        ChatStatusLine(
            message = state.errorMessage ?: state.infoMessage ?: if (state.isListening) "Listening..." else null,
            isError = state.errorMessage != null,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 12.dp, top = 10.dp, bottom = 22.dp),
        ) {
            OutlinedTextField(
                value = state.draft,
                onValueChange = onUpdateDraft,
                placeholder = {
                    Text(
                        "Ask anything...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(28.dp),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                ),
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = false,
                maxLines = 4,
            )
            val isDraftEmpty = state.draft.isBlank()
            VoiceInputButton(
                icon = if (isDraftEmpty) Icons.Default.Mic else Icons.AutoMirrored.Filled.Send,
                isListening = state.isListening,
                modifier = Modifier.size(52.dp),
                onClick = onVoiceInputClick
            )
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessageUiModel,
    onSuggestionClick: (String) -> Unit,
) {
    val isUser = message.sender == Sender.User
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 28.dp,
                topEnd = 28.dp,
                bottomStart = if (isUser) 28.dp else 8.dp,
                bottomEnd = if (isUser) 8.dp else 28.dp,
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = if (!isUser) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
            modifier = Modifier.padding(
                start = if (isUser) 48.dp else 0.dp,
                end = if (isUser) 0.dp else 24.dp
            ),
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isUser) {
                    Text(
                        "AI ASSISTANT",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }
                androidx.compose.material3.Text(
                    text = message.text, 
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                )
                if (!isUser) {
                    message.blocks.forEach { block ->
                        when (block) {
                            is AgentContentBlock.PlainText -> Text(block.value, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            is AgentContentBlock.Warning -> Text("${block.title}: ${block.detail}", color = MaterialTheme.colorScheme.error)
                            is AgentContentBlock.Clarification -> SuggestionChips(block.options, onSuggestionClick)
                            is AgentContentBlock.ExpenseList -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    block.expenses.forEach { expense ->
                                        SectionCard {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Column {
                                                    Text(expense.title, style = MaterialTheme.typography.titleSmall)
                                                    Text("${expense.category} - ${expense.dateLabel.toDisplayDateLabel()}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Text(expense.amount.asCurrency(), style = MaterialTheme.typography.titleSmall)
                                            }
                                        }
                                    }
                                }
                            }
                            is AgentContentBlock.BudgetOverview -> {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    block.budgets.forEach { BudgetProgressCard(it) }
                                }
                            }
                            is AgentContentBlock.CategoryBreakdown -> {
                                SectionCard {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(block.title, style = MaterialTheme.typography.titleMedium)
                                        block.items.take(4).forEach { category ->
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text(category.category, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text(category.amount.asCurrency(), fontWeight = FontWeight.SemiBold)
                                            }
                                        }
                                    }
                                }
                            }
                            is AgentContentBlock.TrendChart -> TrendChartCard(block.points)
                            is AgentContentBlock.Success -> Text("${block.title}: ${block.detail}")
                            is AgentContentBlock.TotalsSummary -> Text("${block.title}: ${block.amount.asCurrency()} - ${block.subtitle}")
                            is AgentContentBlock.Insight -> Text(block.detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        Text(
            message.timestampLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
        )
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
        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
    )
}

private fun speechIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Constants.VoiceLocale)
    putExtra(RecognizerIntent.EXTRA_PROMPT, "Say an expense or ask your assistant")
}

@Preview(showBackground = true)
@Composable
fun ChatScreenPreview() {
    AIExpenseVoiceTheme {
        ChatScreenContent(
            state = ChatUiState(
                messages = listOf(
                    ChatMessageUiModel(
                        id = "1",
                        sender = Sender.Assistant,
                        text = "Hi! I'm your financial copilot. Ask me anything about your spending.",
                        timestampLabel = "10:00 AM"
                    ),
                    ChatMessageUiModel(
                        id = "2",
                        sender = Sender.User,
                        text = "What was my last expense?",
                        timestampLabel = "10:01 AM"
                    ),
                    ChatMessageUiModel(
                        id = "3",
                        sender = Sender.Assistant,
                        text = "Your last expense was Pizza for $250.00 in Food category.",
                        timestampLabel = "10:02 AM"
                    )
                )
            ),
            onSendMessage = {},
            onUpdateDraft = {},
            onVoiceInputClick = {}
        )
    }
}
