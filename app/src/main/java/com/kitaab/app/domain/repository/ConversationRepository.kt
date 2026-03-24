package com.kitaab.app.domain.repository

import com.kitaab.app.domain.model.Conversation
import com.kitaab.app.domain.model.ConversationWithDetails
import com.kitaab.app.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ConversationRepository {
    suspend fun getOrCreateConversation(listingId: String, sellerId: String): Result<Conversation>
    suspend fun getConversations(userId: String): Result<List<ConversationWithDetails>>
    suspend fun getMessages(conversationId: String): Result<List<Message>>
    suspend fun sendMessage(conversationId: String, text: String): Result<Message>
    suspend fun markMessagesRead(conversationId: String, currentUserId: String): Result<Unit>
    fun subscribeToMessages(conversationId: String): Flow<Message>
}