package com.kitaab.app.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitaab.app.domain.model.Listing
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.TextSearchType
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PAGE_SIZE = 10

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val supabase: SupabaseClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(value: String) {
        _uiState.update { it.copy(query = value) }
    }

    fun onSearchSubmit() {
        search(reset = true)
    }

    fun onClearQuery() {
        _uiState.update { it.copy(query = "") }
        // Re-run with filters only if filters are active, otherwise clear results
        if (_uiState.value.filters.isActive) {
            search(reset = true)
        } else {
            _uiState.update { it.copy(listings = emptyList(), hasSearched = false) }
        }
    }

    fun onToggleFilters() {
        _uiState.update { it.copy(showFilters = !it.showFilters) }
    }

    fun onTypeFilterChanged(type: String?) {
        _uiState.update { it.copy(filters = it.filters.copy(type = type)) }
    }

    fun onConditionFilterChanged(condition: String?) {
        _uiState.update { it.copy(filters = it.filters.copy(condition = condition)) }
    }

    fun onExamTagFilterChanged(tag: String?) {
        _uiState.update { it.copy(filters = it.filters.copy(examTag = tag)) }
    }

    fun onMinPriceChanged(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d{0,6}\$"))) {
            _uiState.update { it.copy(filters = it.filters.copy(minPrice = value)) }
        }
    }

    fun onMaxPriceChanged(value: String) {
        if (value.isEmpty() || value.matches(Regex("^\\d{0,6}\$"))) {
            _uiState.update { it.copy(filters = it.filters.copy(maxPrice = value)) }
        }
    }

    fun onApplyFilters() {
        _uiState.update { it.copy(showFilters = false) }
        // Always search when applying filters — even with empty query show filtered results
        search(reset = true)
    }

    fun onClearFilters() {
        _uiState.update { it.copy(filters = ExploreFilters()) }
        if (_uiState.value.query.isNotBlank()) {
            search(reset = true)
        } else {
            _uiState.update { it.copy(listings = emptyList(), hasSearched = false) }
        }
    }

    fun search(reset: Boolean = true) {
        val state = _uiState.value
        if (!reset && (!state.hasMorePages || state.isLoadingMore)) return

        searchJob?.cancel()
        val page = if (reset) 0 else state.currentPage
        val from = page * PAGE_SIZE
        val to = from + PAGE_SIZE - 1

        searchJob = viewModelScope.launch {
            _uiState.update {
                if (reset) it.copy(isLoading = true, listings = emptyList(), currentPage = 0)
                else it.copy(isLoadingMore = true)
            }

            runCatching {
                val filters = _uiState.value.filters
                val query = _uiState.value.query.trim()

                supabase.postgrest["listings"]
                    .select {
                        filter {
                            eq("status", "ACTIVE")

                            if (query.isNotBlank()) {
                                val tsQuery = query.trim()
                                    .split(Regex("\\s+"))
                                    .filter { it.isNotBlank() }
                                    .joinToString(" | ") { "$it:*" }

                                // Full-text search handles most cases.
                                // OR fallback: ilike on raw author field catches abbreviations
                                // like "HC" matching "Harish Chandra Verma" via partial string.
                                or {
                                    textSearch(
                                        column = "search_text",
                                        query = tsQuery,
                                        config = "english",
                                        textSearchType = TextSearchType.WEBSEARCH,
                                    )
                                    ilike("author", "%${query.trim()}%")
                                    ilike("title", "%${query.trim()}%")
                                }
                            }

                            if (filters.type != null) eq("type", filters.type)
                            if (filters.condition != null) eq("condition", filters.condition)
                            if (filters.examTag != null) {
                                contains("exam_tags", listOf(filters.examTag))
                            }
                            val minP = filters.minPrice.toDoubleOrNull()
                            val maxP = filters.maxPrice.toDoubleOrNull()
                            if (minP != null) gte("price", minP)
                            if (maxP != null) lte("price", maxP)

                        }
                        order("created_at", order = Order.DESCENDING)
                        range(from.toLong(), to.toLong())
                    }
                    .decodeList<Listing>()
            }.fold(
                onSuccess = { newListings ->
                    _uiState.update { current ->
                        current.copy(
                            listings = if (reset) newListings
                            else (current.listings + newListings).distinctBy { it.id },
                            isLoading = false,
                            isLoadingMore = false,
                            hasMorePages = newListings.size == PAGE_SIZE,
                            currentPage = page + 1,
                            hasSearched = true,
                            error = null,
                        )
                    }
                },
                onFailure = { cause ->
                    _uiState.update {
                        it.copy(isLoading = false, isLoadingMore = false, error = cause.message)
                    }
                },
            )
        }
    }

    fun loadNextPage() {
        search(reset = false)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}