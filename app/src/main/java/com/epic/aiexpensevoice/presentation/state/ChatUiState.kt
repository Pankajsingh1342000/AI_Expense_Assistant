package com.epic.aiexpensevoice.presentation.state

import com.epic.aiexpensevoice.domain.model.ChatMessageUiModel

data class ChatUiState(
    val draft: String = "",
    val isSending: Boolean = false,
    val isListening: Boolean = false,
    val transcript: String? = null,
    val hasVoicePermission: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val messages: List<ChatMessageUiModel> = emptyList(),
)
