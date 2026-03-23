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
    @SerialName("profile_photo_url") val profilePhotoUrl: String? = null,
    @SerialName("avg_rating") val avgRating: Double = 0.0,
    @SerialName("review_count") val reviewCount: Int = 0,
    val badge: String? = null,
)