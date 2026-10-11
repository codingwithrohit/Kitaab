package com.kitaab.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class CachedUserProfile(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val city: String?,
    val pincode: String?,
    val locality: String?,
    val profilePhotoUrl: String?,
    val avgRating: Double,
    val reviewCount: Int,
    val badge: String?,
    val examTags: List<String>,
    val totalSold: Int,
    val totalDonated: Int,
    val strikeCount: Int,
    val isSuspended: Boolean,
    val createdAt: String?,
    val cachedAt: Long = System.currentTimeMillis(),
)
