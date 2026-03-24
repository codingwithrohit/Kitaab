package com.kitaab.app.feature.chat

import com.kitaab.app.domain.model.Message

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
    val currentUserId: String = "",
)


sealed interface ChatEvent {
    data object ScrollToBottom : ChatEvent
}