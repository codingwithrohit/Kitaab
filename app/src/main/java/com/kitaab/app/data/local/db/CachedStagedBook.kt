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
    val bundleId: String?,
    val title: String,
    val author: String,
    val publisher: String,
    val edition: String,
    val isbn: String,
    val subject: String,
    val examTags: List<String>,
    val hasSolutions: Boolean,
    val hasNotes: Boolean,
    val condition: String,
    val individualPrice: String,
    val photoPaths: List<String>,
    val typeOverride: String?,
    val sortOrder: Int,
)
