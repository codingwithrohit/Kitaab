package com.kitaab.app.domain.model

data class ConversationWithDetails(
    val conversation: Conversation,
    val listingTitle: String,
    val listingCoverUrl: String?,
    val otherPersonName: String,
    val otherPersonId: String,
    val unreadCount: Int,
)