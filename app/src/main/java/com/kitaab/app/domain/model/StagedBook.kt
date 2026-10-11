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
    val individualPrice: String = "",
    val photoPaths: List<String> = emptyList(),
    val typeOverride: ListingType? = null,
    val sortOrder: Int = 0,
) {
    val coverPhotoPath: String? get() = photoPaths.firstOrNull()

    // Always requires session default — never falls back to SELL silently
    fun effectiveType(sessionDefault: ListingType): ListingType = typeOverride ?: sessionDefault

    fun isReadyToPublish(sessionDefault: ListingType): Boolean {
        val type = effectiveType(sessionDefault)
        val priceOk = type == ListingType.DONATE || individualPrice.isNotBlank()
        return title.isNotBlank() && condition != null && priceOk
    }

    // Warning text shown in ReviewPublishScreen per book
    fun warningText(sessionDefault: ListingType): String? {
        val type = effectiveType(sessionDefault)
        return when {
            title.isBlank() -> "Title required"
            condition == null -> "Condition required"
            type == ListingType.SELL && individualPrice.isBlank() -> "Price required"
            else -> null
        }
    }
}
