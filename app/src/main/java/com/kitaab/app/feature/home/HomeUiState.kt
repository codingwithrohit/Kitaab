package com.kitaab.app.feature.home

import com.kitaab.app.domain.model.Listing
import com.kitaab.app.domain.model.UserProfile

data class HomeUiState(
    val userProfile: UserProfile? = null,
    val listings: List<Listing> = emptyList(),
    val selectedExamTag: String = "All",
    val isLoadingProfile: Boolean = true,
    val isLoadingListings: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val error: String? = null,
    val currentPage: Int = 0,
)