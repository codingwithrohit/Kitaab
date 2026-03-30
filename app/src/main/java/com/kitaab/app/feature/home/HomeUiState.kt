package com.kitaab.app.feature.home

import com.kitaab.app.domain.model.Listing
import com.kitaab.app.domain.model.UserProfile

enum class HomeViewMode { LIST, GRID }

enum class HomeSortOption(val label: String) {
    RECENT("Recent"),
    OLDEST("Oldest"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    NEARBY("Nearby"),
}

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
    val viewMode: HomeViewMode = HomeViewMode.LIST,
    val sortOption: HomeSortOption = HomeSortOption.RECENT,
) {
    val sortedListings: List<Listing> get() = when (sortOption) {
        HomeSortOption.RECENT -> listings.sortedByDescending { it.createdAt }
        HomeSortOption.OLDEST -> listings.sortedBy { it.createdAt }
        HomeSortOption.PRICE_LOW_HIGH -> listings.sortedWith(
            compareBy(nullsLast()) { it.price }
        )
        HomeSortOption.PRICE_HIGH_LOW -> listings.sortedWith(
            compareByDescending(nullsFirst()) { it.price }
        )
        HomeSortOption.NEARBY -> listings
    }
}