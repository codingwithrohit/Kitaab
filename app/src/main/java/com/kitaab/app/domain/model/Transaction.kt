package com.kitaab.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Transaction(
    val id: String,
    @SerialName("listing_id") val listingId: String,
    @SerialName("seller_id") val sellerId: String,
    @SerialName("buyer_id") val buyerId: String,
    val type: String,
    @SerialName("handoff_method") val handoffMethod: String? = null,
    @SerialName("handoff_code") val handoffCode: String? = null,
    @SerialName("packed_photo_url") val packedPhotoUrl: String? = null,
    @SerialName("confirmed_by_seller") val confirmedBySeller: Boolean = false,
    @SerialName("confirmed_by_buyer") val confirmedByBuyer: Boolean = false,
    @SerialName("completed_at") val completedAt: String? = null,
    val disputed: Boolean = false,
    @SerialName("conversation_id") val conversationId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)