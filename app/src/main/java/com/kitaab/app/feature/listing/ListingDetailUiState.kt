package com.kitaab.app.feature.listing

import com.kitaab.app.domain.model.Listing
import com.kitaab.app.domain.model.UserProfile

data class ListingDetailUiState(
    val listing: Listing? = null,
    val seller: UserProfile? = null,
    val similarListings: List<Listing> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isOwnListing: Boolean = false,
)
