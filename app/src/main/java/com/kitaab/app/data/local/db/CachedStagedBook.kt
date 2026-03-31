package com.kitaab.app.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "staged_books",
    foreignKeys = [
        ForeignKey(
            entity = CachedPostingSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId"), Index("bundleId")],
)
data class CachedStagedBook(
    @PrimaryKey val id: String,
    val sessionId: String,
    val bundleId: String?,              // null = individual listing
    val title: String,
    val author: String,
    val publisher: String,
    val edition: String,
    val isbn: String,
    val subject: String,
    val examTags: List<String>,         // converted via Converters
    val hasSolutions: Boolean,
    val hasNotes: Boolean,
    val condition: String,              // BookCondition.name
    val individualPrice: String,        // raw user input; empty string = no price
    // Stable internal-storage file paths copied immediately on book confirm.
    // Never the original picker URIs — those can be invalidated by the system.
    val photoPaths: List<String>,       // converted via Converters
    val typeOverride: String?,          // null = use session default; "SELL" | "DONATE"
    val sortOrder: Int,                 // preserves insertion order within a session / bundle
)