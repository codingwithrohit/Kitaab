package com.kitaab.app.feature.listing

sealed interface ListingDetailEvent {
    data class NavigateToChat(val conversationId: String) : ListingDetailEvent
    data class NavigateToDonationRequest(val listingId: String) : ListingDetailEvent
}