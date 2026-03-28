package com.kitaab.app.feature.listing

sealed interface ListingDetailEvent {
    data class NavigateToChat(val conversationId: String) : ListingDetailEvent
    data class NavigateToDonationRequest(val listingId: String) : ListingDetailEvent  // buyer bottom sheet
    data class NavigateToDonationRequests(val listingId: String) : ListingDetailEvent // seller full screen
}