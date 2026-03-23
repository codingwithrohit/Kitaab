package com.kitaab.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Listing(
    val id: String,
    @SerialName("seller_id") val sellerId: String,
    val title: String,
    val author: String? = null,
    val publisher: String? = null,
    val edition: String? = null,
    val isbn: String? = null,
    val subject: String? = null,
    @SerialName("exam_tags") val examTags: List<String> = emptyList(),
    val condition: String,
    val type: String,
    val price: Double? = null,
    val description: String? = null,
    @SerialName("photo_urls") val photoUrls: List<String> = emptyList(),
    @SerialName("has_solutions") val hasSolutions: Boolean = false,
    @SerialName("has_notes") val hasNotes: Boolean = false,
    @SerialName("toc_photo_url") val tocPhotoUrl: String? = null,
    val status: String,
    val city: String? = null,
    val pincode: String? = null,
    val locality: String? = null,
    @SerialName("is_bundle") val isBundle: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)