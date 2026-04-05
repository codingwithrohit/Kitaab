package com.kitaab.app.feature.post.multi

import android.net.Uri
import com.kitaab.app.feature.post.BookCondition
import com.kitaab.app.feature.post.ListingType

data class AddBookSheetState(
    val isVisible: Boolean = false,
    val editingBookId: String? = null,
    // Fields
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
    val photoUris: List<Uri> = emptyList(),
    val typeOverride: ListingType? = null,
    // Async states
    val isFetchingBookDetails: Boolean = false,
    val bookNotFound: Boolean = false,
    val isCopyingPhotos: Boolean = false,
    // Validation errors
    val titleError: String? = null,
    val conditionError: String? = null,
    val priceError: String? = null,
) {
    val isEditing: Boolean get() = editingBookId != null
}
