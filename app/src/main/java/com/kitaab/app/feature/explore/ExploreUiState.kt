package com.kitaab.app.feature.explore

import com.kitaab.app.domain.model.Listing

data class ExploreUiState(
    val query: String = "",
    val filters: ExploreFilters = ExploreFilters(),
    val listings: List<Listing> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val currentPage: Int = 0,
    val showFilters: Boolean = false,
    val error: String? = null,
    val hasSearched: Boolean = false,
)