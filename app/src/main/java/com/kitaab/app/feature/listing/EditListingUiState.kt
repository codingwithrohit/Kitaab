package com.kitaab.app.feature.listing

import android.net.Uri
import com.kitaab.app.feature.post.BookCondition
import com.kitaab.app.feature.post.ListingType

data class EditListingUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val error: String? = null,
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
    val listingType: ListingType = ListingType.SELL,
    val price: String = "",
    val city: String = "",
    val pincode: String = "",
    val locality: String = "",
    val existingPhotoUrls: List<String> = emptyList(),
    val newPhotoUris: List<Uri> = emptyList(),
    val isFetchingBookDetails: Boolean = false,
    val bookNotFound: Boolean = false,
    val titleError: String? = null,
    val conditionError: String? = null,
    val priceError: String? = null,
    val cityError: String? = null,
    val pincodeError: String? = null,
) {
    // Combined display list: existing URLs first (shown as remote images), then new URIs
    val totalPhotoCount: Int get() = existingPhotoUrls.size + newPhotoUris.size
    val canAddMorePhotos: Boolean get() = totalPhotoCount < 5
}

sealed interface EditListingEvent {
    data object SaveSuccess : EditListingEvent
}
