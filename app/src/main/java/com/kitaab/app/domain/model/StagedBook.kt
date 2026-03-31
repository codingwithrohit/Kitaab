package com.kitaab.app.domain.model

import com.kitaab.app.feature.post.BookCondition
import com.kitaab.app.feature.post.ListingType

data class StagedBook(
    val id: String,
    val sessionId: String,
    val bundleId: String? = null,
    val title: String = "",
    val author: String = "",
    val publisher: String = "",
    val edition: String = "",
    val isbn: String = "",
    val subject: String = "",
    val examTags: Set<String> = emptySet(),
    val hasSolutions: Boolean = false,
    val hasNotes: Boolean = false,
    val condition: BookCondition? = null,
    // Raw price string — empty means not yet set (valid for donations)
    val individualPrice: String = "",
    // Stable internal-storage file paths — NOT picker URIs
    val photoPaths: List<String> = emptyList(),
    val typeOverride: ListingType? = null,           // null = use session default
    val sortOrder: Int = 0,
) {
    // First photo path is always the cover
    val coverPhotoPath: String? get() = photoPaths.firstOrNull()

    val effectiveType: ListingType
        get() = typeOverride ?: ListingType.SELL     // caller passes session default when needed

    val isReadyToPublish: Boolean
        get() = title.isNotBlank() && condition != null
}