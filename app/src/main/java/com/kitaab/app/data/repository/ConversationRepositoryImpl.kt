package com.kitaab.app.data.repository

import com.kitaab.app.domain.model.Conversation
import com.kitaab.app.domain.model.ConversationWithDetails
import com.kitaab.app.domain.model.Listing
import com.kitaab.app.domain.model.Message
import com.kitaab.app.domain.model.UserProfile
import com.kitaab.app.domain.repository.ConversationRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import javax.inject.Inject

class ConversationRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
) : ConversationRepository {

    private val currentUserId get() = supabase.auth.currentUserOrNull()?.id ?: ""

    override suspend fun getOrCreateConversation(
        listingId: String,
        sellerId: String,
    ): Result<Conversation> = runCatching {
        val existing = supabase.postgrest["conversations"]
            .select {
                filter {
                    eq("listing_id", listingId)
                    eq("buyer_id", currentUserId)
                }
            }
            .decodeList<Conversation>()
            .firstOrNull()

        if (existing != null) return@runCatching existing

        supabase.postgrest["conversations"]
            .insert(
                mapOf(
                    "listing_id" to listingId,
                    "buyer_id" to currentUserId,
                    "seller_id" to sellerId,
                )
            ) {
                select()
            }
            .decodeList<Conversation>()
            .first()
    }

    override suspend fun getConversations(userId: String): Result<List<ConversationWithDetails>> =
        runCatching {
            val conversations = supabase.postgrest["conversations"]
                .select {
                    filter {
                        or {
                            eq("buyer_id", userId)
                            eq("seller_id", userId)
                        }
                    }
                    order("last_message_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<Conversation>()

            conversations.map { conv ->
                val listing = runCatching {
                    supabase.postgrest["listings"]
                        .select { filter { eq("id", conv.listingId) } }
                        .decodeList<Listing>()
                        .firstOrNull()
                }.getOrNull()

                val listingTitle = listing?.title ?: "Listing"
                val listingCoverUrl = listing?.photoUrls?.firstOrNull()

                val otherPersonId = if (conv.buyerId == userId) conv.sellerId else conv.buyerId
                val otherPersonName = runCatching {
                    supabase.postgrest["users"]
                        .select { filter { eq("id", otherPersonId) } }
                        .decodeList<UserProfile>()
                        .firstOrNull()?.name ?: "User"
                }.getOrElse { "User" }

                val unreadCount = runCatching {
                    supabase.postgrest["messages"]
                        .select {
                            filter {
                                eq("conversation_id", conv.id)
                                eq("is_read", false)
                                neq("sender_id", userId)
                            }
                        }
                        .decodeList<Message>()
                        .size
                }.getOrElse { 0 }

                ConversationWithDetails(
                    conversation = conv,
                    listingTitle = listingTitle,
                    listingCoverUrl = listingCoverUrl,
                    otherPersonName = otherPersonName,
                    otherPersonId = otherPersonId,
                    unreadCount = unreadCount,
                )
            }
        }

    override suspend fun getMessages(conversationId: String): Result<List<Message>> =
        runCatching {
            supabase.postgrest["messages"]
                .select {
                    filter { eq("conversation_id", conversationId) }
                    order("sent_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                }
                .decodeList<Message>()
        }

    override suspend fun sendMessage(conversationId: String, text: String): Result<Message> =
        runCatching {
            val message = supabase.postgrest["messages"]
                .insert(
                    mapOf(
                        "conversation_id" to conversationId,
                        "sender_id" to currentUserId,
                        "text" to text,
                    )
                ) {
                    select()
                }
                .decodeList<Message>()
                .first()

            // Update conversation last_message
            supabase.postgrest["conversations"]
                .update(
                    mapOf(
                        "last_message" to text,
                        "last_message_at" to message.sentAt,
                    )
                ) {
                    filter { eq("id", conversationId) }
                }

            message
        }

    override suspend fun markMessagesRead(
        conversationId: String,
        currentUserId: String,
    ): Result<Unit> = runCatching {
        supabase.postgrest["messages"]
            .update(mapOf("is_read" to true)) {
                filter {
                    eq("conversation_id", conversationId)
                    eq("is_read", false)
                    neq("sender_id", currentUserId)
                }
            }
    }

    override fun subscribeToMessages(conversationId: String): Flow<Message> {
        val channel = supabase.realtime.channel("messages:$conversationId")
        val flow = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
            table = "messages"
            filter("conversation_id", FilterOperator.EQ, conversationId)
        }.mapNotNull { action ->
            runCatching {
                Json.decodeFromString<Message>(action.record.toString())
            }.getOrNull()
        }

        return kotlinx.coroutines.flow.callbackFlow {
            val job = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                flow.collect { send(it) }
            }
            channel.subscribe()
            awaitClose {
                job.cancel()
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    channel.unsubscribe()
                }
            }
        }
    }
}