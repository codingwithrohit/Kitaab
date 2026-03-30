package com.kitaab.app.feature.chat

import com.kitaab.app.domain.model.Message
import com.kitaab.app.domain.model.Transaction

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val error: String? = null,
    val currentUserId: String = "",
    val isSeller: Boolean = false,
    val transaction: Transaction? = null,
    val isCreatingTransaction: Boolean = false,
    val isConfirming: Boolean = false,
    val isDisputing: Boolean = false,
    val showHandoffSheet: Boolean = false,
    val listingId: String = "",
    val otherUserId: String = "",
    val listingType: String = "SELL",
)

sealed interface ChatEvent {
    data object ScrollToBottom : ChatEvent

    data object TransactionComplete : ChatEvent
}
