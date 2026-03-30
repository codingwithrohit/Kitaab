package com.kitaab.app.domain.model

data class DonationRequestWithRequester(
    val request: DonationRequest,
    val requesterName: String,
    val requesterCity: String?,
    val requesterBadge: String?,
    val requesterCreatedAt: String?,
)
