
package com.kitaab.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitaab.app.domain.model.Listing
import com.kitaab.app.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PAGE_SIZE = 10

@HiltViewModel
class HomeViewModel
@Inject
constructor(
    private val supabase: SupabaseClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    // Invalidates responses from older listing requests.
    private var listingsRequestVersion = 0

    init {
        loadUserProfile()
        loadListings(reset = true)
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            val userId =
                supabase.auth.currentSessionOrNull()?.user?.id
                    ?: run {
                        _uiState.update {
                            it.copy(isLoadingProfile = false)
                        }
                        return@launch
                    }

            runCatching {
                supabase.postgrest["users"]
                    .select {
                        filter {
                            eq("id", userId)
                        }
                    }
                    .decodeSingle<UserProfile>()
            }.onSuccess { profile ->
                _uiState.update {
                    it.copy(
                        userProfile = profile,
                        isLoadingProfile = false,
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(isLoadingProfile = false)
                }
            }
        }
    }

    fun onExamTagSelected(tag: String) {
        if (_uiState.value.selectedExamTag == tag) return

        _uiState.update {
            it.copy(selectedExamTag = tag)
        }

        loadListings(reset = true)
    }

    fun loadListings(reset: Boolean = false) {
        val state = _uiState.value

        // Prevent duplicate page requests.
        if (!reset &&
            (!state.hasMorePages ||
                    state.isLoadingMore ||
                    state.isLoadingListings)
        ) {
            return
        }

        // A reset invalidates any older request.
        if (reset) {
            listingsRequestVersion++
        }

        val requestVersion = listingsRequestVersion
        val selectedExamTag = state.selectedExamTag
        val page = if (reset) 0 else state.currentPage
        val from = page * PAGE_SIZE
        val to = from + PAGE_SIZE - 1

        // Update synchronously, before launching the coroutine.
        _uiState.update {
            if (reset) {
                it.copy(
                    isLoadingListings = true,
                    isLoadingMore = false,
                    listings = emptyList(),
                    currentPage = 0,
                    hasMorePages = true,
                    error = null,
                )
            } else {
                it.copy(
                    isLoadingMore = true,
                    error = null,
                )
            }
        }

        viewModelScope.launch {
            val result = runCatching {
                supabase.postgrest["listings"]
                    .select {
                        filter {
                            eq("status", "ACTIVE")

                            if (selectedExamTag != "All") {
                                contains(
                                    "exam_tags",
                                    listOf(selectedExamTag),
                                )
                            }
                        }

                        order(
                            "created_at",
                            order = Order.DESCENDING,
                        )

                        range(from.toLong(), to.toLong())
                    }
                    .decodeList<Listing>()
            }

            // Ignore responses belonging to an older filter/reset.
            if (requestVersion != listingsRequestVersion) {
                return@launch
            }

            result.onSuccess { newListings ->
                _uiState.update { current ->
                    current.copy(
                        listings = if (reset) {
                            newListings.distinctBy { it.id }
                        } else {
                            (current.listings + newListings)
                                .distinctBy { it.id }
                        },
                        isLoadingListings = false,
                        isLoadingMore = false,
                        hasMorePages = newListings.size == PAGE_SIZE,
                        currentPage = page + 1,
                        error = null,
                    )
                }
            }.onFailure { cause ->
                _uiState.update {
                    it.copy(
                        isLoadingListings = false,
                        isLoadingMore = false,
                        error = cause.message
                            ?: "Unable to load listings. Please try again.",
                    )
                }
            }
        }
    }

    fun onViewModeToggled() {
        _uiState.update {
            it.copy(
                viewMode = if (it.viewMode == HomeViewMode.LIST) {
                    HomeViewMode.GRID
                } else {
                    HomeViewMode.LIST
                },
            )
        }
    }

    fun onSortOptionSelected(option: HomeSortOption) {
        _uiState.update {
            it.copy(sortOption = option)
        }
    }

    fun loadNextPage() {
        loadListings(reset = false)
    }

    fun refresh() {
        loadUserProfile()
        loadListings(reset = true)
    }

    fun clearError() {
        _uiState.update {
            it.copy(error = null)
        }
    }
}
