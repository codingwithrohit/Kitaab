package com.kitaab.app.feature.donation

sealed interface DonationRequestEvent {
    data object SubmitSuccess : DonationRequestEvent
}