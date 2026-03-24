package com.kitaab.app.feature.listing

sealed interface ListingDetailEvent {
    data class NavigateToChat(val sellerId: String, val listingId: String) : ListingDetailEvent
    data class NavigateToDonationRequest(val listingId: String) : ListingDetailEvent
}