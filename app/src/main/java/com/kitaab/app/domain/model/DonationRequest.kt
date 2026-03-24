package com.kitaab.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DonationRequest(
    val id: String,
    @SerialName("listing_id") val listingId: String,
    @SerialName("requester_id") val requesterId: String,
    val reason: String,
    @SerialName("exam_tag") val examTag: String? = null,
    val status: String = "PENDING",
    @SerialName("created_at") val createdAt: String? = null,
)