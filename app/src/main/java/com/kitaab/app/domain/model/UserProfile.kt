package com.kitaab.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val city: String? = null,
    val pincode: String? = null,
    val locality: String? = null,
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
    @SerialName("avg_rating") val avgRating: Double = 0.0,
    @SerialName("review_count") val reviewCount: Int = 0,
    val badge: String? = null,
    @SerialName("exam_tags") val examTags: List<String> = emptyList(),
    @SerialName("total_sold") val totalSold: Int = 0,
    @SerialName("total_donated") val totalDonated: Int = 0,
    @SerialName("strike_count") val strikeCount: Int = 0,
    @SerialName("is_suspended") val isSuspended: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)
