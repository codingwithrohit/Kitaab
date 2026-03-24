package com.kitaab.app.feature.inbox

import com.kitaab.app.domain.model.ConversationWithDetails

data class InboxUiState(
    val conversations: List<ConversationWithDetails> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
)