package com.kitaab.app.data.repository

import com.kitaab.app.domain.model.Transaction
import com.kitaab.app.domain.repository.TransactionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import javax.inject.Inject

class TransactionRepositoryImpl
    @Inject
    constructor(
        private val supabase: SupabaseClient,
    ) : TransactionRepository {
        private val currentUserId get() = supabase.auth.currentUserOrNull()?.id ?: ""

        override suspend fun getTransactionForConversation(conversationId: String): Result<Transaction?> =
            runCatching {
                supabase.postgrest["transactions"]
                    .select {
                        filter { eq("conversation_id", conversationId) }
                    }
                    .decodeList<Transaction>()
                    .firstOrNull()
            }

        override suspend fun createTransaction(
            conversationId: String,
            listingId: String,
            sellerId: String,
            buyerId: String,
            type: String,
            handoffMethod: String,
        ): Result<Transaction> =
            runCatching {
                val handoffCode =
                    if (handoffMethod == "PORTER") {
                        generateHandoffCode()
                    } else {
                        null
                    }

                supabase.postgrest["transactions"]
                    .insert(
                        mapOf(
                            "conversation_id" to conversationId,
                            "listing_id" to listingId,
                            "seller_id" to sellerId,
                            "buyer_id" to buyerId,
                            "type" to type,
                            "handoff_method" to handoffMethod,
                            "handoff_code" to handoffCode,
                        ),
                    ) {
                        select()
                    }
                    .decodeList<Transaction>()
                    .first()
            }

        override suspend fun confirmByCurrentUser(
            transactionId: String,
            isSeller: Boolean,
        ): Result<Transaction> =
            runCatching {
                val field = if (isSeller) "confirmed_by_seller" else "confirmed_by_buyer"

                val updated =
                    supabase.postgrest["transactions"]
                        .update(mapOf(field to true)) {
                            filter { eq("id", transactionId) }
                            select()
                        }
                        .decodeList<Transaction>()
                        .first()

                // If both confirmed — mark complete, update listing, update badge
                if (updated.confirmedBySeller && updated.confirmedByBuyer) {
                    val sdf =
                        java.text.SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss'Z'",
                            java.util.Locale.getDefault(),
                        )
                    sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    val now = sdf.format(java.util.Date())

                    supabase.postgrest["transactions"]
                        .update(mapOf("completed_at" to now)) {
                            filter { eq("id", transactionId) }
                        }

                    supabase.postgrest["listings"]
                        .update(mapOf("status" to "COMPLETED")) {
                            filter { eq("id", updated.listingId) }
                        }

                    if (updated.type == "DONATE") {
                        supabase.postgrest.rpc(
                            "increment_total_donated",
                            mapOf("p_user_id" to updated.sellerId),
                        )
                        supabase.postgrest.rpc(
                            "update_user_badge",
                            mapOf("p_user_id" to updated.sellerId),
                        )
                    }

                    if (updated.type == "SELL") {
                        supabase.postgrest.rpc(
                            "increment_total_sold",
                            mapOf("p_user_id" to updated.sellerId),
                        )
                    }
                }

                updated
            }

        override suspend fun uploadPackedPhoto(
            transactionId: String,
            photoBytes: ByteArray,
        ): Result<String> =
            runCatching {
                val path = "packed/$transactionId/packed_photo.jpg"
                supabase.storage.from("book-photos").upload(path, photoBytes) {
                    upsert = true
                }
                val url = supabase.storage.from("book-photos").publicUrl(path)

                supabase.postgrest["transactions"]
                    .update(mapOf("packed_photo_url" to url)) {
                        filter { eq("id", transactionId) }
                    }

                url
            }

        override suspend fun raiseDispute(transactionId: String): Result<Unit> =
            runCatching {
                supabase.postgrest["transactions"]
                    .update(mapOf("disputed" to true)) {
                        filter { eq("id", transactionId) }
                    }
                Unit
            }

        private fun generateHandoffCode(): String {
            val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
            return (1..6).map { chars.random() }.joinToString("")
        }
    }
