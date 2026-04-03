package com.epic.aiexpensevoice.presentation.screen.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epic.aiexpensevoice.core.common.Resource
import com.epic.aiexpensevoice.core.common.nowLabel
import com.epic.aiexpensevoice.data.local.chat.ChatHistoryStore
import com.epic.aiexpensevoice.data.repository.AgentRepository
import com.epic.aiexpensevoice.domain.model.ChatMessageUiModel
import com.epic.aiexpensevoice.domain.model.Sender
import com.epic.aiexpensevoice.presentation.state.ChatUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(
    private val agentRepository: AgentRepository,
    private val chatHistoryStore: ChatHistoryStore,
) : ViewModel() {
    private val initialMessage = ChatMessageUiModel(
        id = UUID.randomUUID().toString(),
        sender = Sender.Assistant,
        text = "Hi, I'm ready whenever you want to log a spend, check a budget, or understand where your money is going.",
        timestampLabel = nowLabel(),
    )

    private val _uiState = MutableStateFlow(ChatUiState(messages = listOf(initialMessage)))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        chatHistoryStore.observeMessages()
            .onEach { cachedMessages ->
                _uiState.value = _uiState.value.copy(
                    messages = if (cachedMessages.isEmpty()) listOf(initialMessage) else cachedMessages,
                )
            }
            .launchIn(viewModelScope)
    }

    fun updateDraft(value: String) {
        _uiState.value = _uiState.value.copy(draft = value, errorMessage = null, infoMessage = null)
    }

    fun setListening(value: Boolean) {
        _uiState.value = _uiState.value.copy(isListening = value, infoMessage = if (value) "Listening..." else null)
    }

    fun updateVoicePermission(granted: Boolean) {
        _uiState.value = _uiState.value.copy(
            hasVoicePermission = granted,
            errorMessage = if (granted) null else _uiState.value.errorMessage,
        )
    }

    fun onTranscriptCaptured(text: String) {
        _uiState.value = _uiState.value.copy(
            transcript = text,
            draft = text,
            isListening = false,
            infoMessage = null,
            errorMessage = null,
        )
        sendMessage(text)
    }

    fun onVoiceInputEmpty() {
        _uiState.value = _uiState.value.copy(
            isListening = false,
            transcript = null,
            infoMessage = "I didn't catch that. Try again when you're ready.",
        )
    }

    fun onVoicePermissionDenied() {
        _uiState.value = _uiState.value.copy(
            isListening = false,
            errorMessage = "Turn on microphone access to use voice chat.",
        )
    }

    fun sendMessage(query: String = _uiState.value.draft) {
        val prompt = query.trim()
        if (prompt.isBlank()) return

        val userMessage = ChatMessageUiModel(
            id = UUID.randomUUID().toString(),
            sender = Sender.User,
            text = prompt,
            timestampLabel = nowLabel(),
        )
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMessage,
            draft = "",
            transcript = null,
            isSending = true,
            errorMessage = null,
            infoMessage = null,
        )
        persistMessages()

        viewModelScope.launch {
            when (val result = agentRepository.sendQuery(prompt)) {
                is Resource.Success -> {
                    val reply = ChatMessageUiModel(
                        id = UUID.randomUUID().toString(),
                        sender = Sender.Assistant,
                        text = result.data.headline,
                        blocks = result.data.blocks,
                        timestampLabel = nowLabel(),
                    )
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        messages = _uiState.value.messages + reply,
                        infoMessage = if (reply.text.contains("offline", ignoreCase = true) || reply.text.contains("queued", ignoreCase = true)) {
                            reply.text
                        } else {
                            null
                        },
                    )
                    persistMessages()
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        errorMessage = result.message,
                        messages = _uiState.value.messages + ChatMessageUiModel(
                            id = UUID.randomUUID().toString(),
                            sender = Sender.Assistant,
                            text = result.message,
                            timestampLabel = nowLabel(),
                        ),
                    )
                    persistMessages()
                }
                Resource.Loading -> Unit
            }
        }
    }

    private fun persistMessages() {
        viewModelScope.launch {
            chatHistoryStore.saveMessages(_uiState.value.messages)
        }
    }
}
