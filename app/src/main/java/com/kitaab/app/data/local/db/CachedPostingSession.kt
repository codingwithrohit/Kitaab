package com.kitaab.app.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posting_sessions")
data class CachedPostingSession(
    @PrimaryKey val id: String,
    // "SELL" | "DONATE"
    val defaultType: String,
    val defaultCity: String,
    val defaultPincode: String,
    val defaultLocality: String,
    val createdAt: Long,
)
