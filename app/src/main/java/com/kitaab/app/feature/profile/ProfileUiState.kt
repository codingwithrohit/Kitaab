package com.kitaab.app.feature.profile

import com.kitaab.app.domain.model.DonationRequest
import com.kitaab.app.domain.model.Listing
import com.kitaab.app.domain.model.UserProfile

data class ProfileUiState(
    val profile: UserProfile? = null,
    val ownListings: List<Listing> = emptyList(),
    val myRequests: List<DonationRequest> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isSigningOut: Boolean = false,
    val isDeletingAccount: Boolean = false,
    val error: String? = null,
    val selectedTab: ProfileTab = ProfileTab.LISTINGS,
)