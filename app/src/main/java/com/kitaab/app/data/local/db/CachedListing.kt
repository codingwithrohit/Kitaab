package com.kitaab.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "own_listings")
data class CachedListing(
    @PrimaryKey val id: String,
    val sellerId: String,
    val title: String,
    val author: String?,
    val publisher: String?,
    val edition: String?,
    val isbn: String?,
    val subject: String?,
    val examTags: List<String>,
    val condition: String,
    val type: String,
    val price: Double?,
    val photoUrls: List<String>,
    val hasSolutions: Boolean,
    val hasNotes: Boolean,
    val status: String,
    val city: String?,
    val pincode: String?,
    val locality: String?,
    val isBundle: Boolean,
    val createdAt: String?,
    val cachedAt: Long = System.currentTimeMillis(),
)