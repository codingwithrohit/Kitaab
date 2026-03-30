package com.kitaab.app.feature.donation

import com.kitaab.app.domain.model.DonationRequestWithRequester

data class DonationRequestsSellerUiState(
    val requests: List<DonationRequestWithRequester> = emptyList(),
    val isLoading: Boolean = false,
    val isAccepting: String? = null,
    val error: String? = null,
    val acceptedRequestId: String? = null,
)
