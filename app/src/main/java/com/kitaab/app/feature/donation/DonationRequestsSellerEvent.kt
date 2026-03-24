package com.kitaab.app.feature.donation

sealed interface DonationRequestsSellerEvent {
    data object AcceptSuccess : DonationRequestsSellerEvent
}