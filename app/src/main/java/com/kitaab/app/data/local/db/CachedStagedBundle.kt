package com.kitaab.app.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "staged_bundles",
    foreignKeys = [
        ForeignKey(
            entity = CachedPostingSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class CachedStagedBundle(
    @PrimaryKey val id: String,
    val sessionId: String,
    val name: String,
    val bundlePrice: String,            // raw user input; empty string = not yet set
    val typeOverride: String?,          // null = use session default; "SELL" | "DONATE"
)