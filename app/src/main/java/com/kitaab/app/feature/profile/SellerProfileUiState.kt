package com.kitaab.app.feature.profile

import com.kitaab.app.domain.model.Listing
import com.kitaab.app.domain.model.UserProfile

data class SellerProfileUiState(
    val profile: UserProfile? = null,
    val listings: List<Listing> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)
