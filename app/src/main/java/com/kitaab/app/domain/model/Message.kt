package com.kitaab.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("sender_id") val senderId: String,
    val text: String,
    @SerialName("sent_at") val sentAt: String,
    @SerialName("is_read") val isRead: Boolean = false,
)
