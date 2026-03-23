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
class HomeViewModel @Inject constructor(
    private val supabase: SupabaseClient,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadUserProfile()
        loadListings(reset = true)
    }

    fun loadUserProfile() {
        viewModelScope.launch {
            val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return@launch
            runCatching {
                supabase.postgrest["users"]
                    .select { filter { eq("id", userId) } }
                    .decodeSingle<UserProfile>()
            }.onSuccess { profile ->
                _uiState.update { it.copy(userProfile = profile, isLoadingProfile = false) }
            }.onFailure {
                _uiState.update { it.copy(isLoadingProfile = false) }
            }
        }
    }

    fun onExamTagSelected(tag: String) {
        if (_uiState.value.selectedExamTag == tag) return
        _uiState.update { it.copy(selectedExamTag = tag) }
        loadListings(reset = true)
    }

    fun loadListings(reset: Boolean = false) {
        val state = _uiState.value
        if (!reset && (!state.hasMorePages || state.isLoadingMore)) return

        val page = if (reset) 0 else state.currentPage
        val from = page * PAGE_SIZE
        val to = from + PAGE_SIZE - 1

        viewModelScope.launch {
            _uiState.update {
                if (reset) it.copy(isLoadingListings = true, listings = emptyList(), currentPage = 0)
                else it.copy(isLoadingMore = true)
            }

            runCatching {
                supabase.postgrest["listings"]
                    .select {
                        filter {
                            eq("status", "ACTIVE")
                            if (state.selectedExamTag != "All") {
                                contains("exam_tags", listOf(state.selectedExamTag))
                            }
                        }
                        order("created_at", order = Order.DESCENDING)
                        range(from.toLong(), to.toLong())
                    }
                    .decodeList<Listing>()
            }.onSuccess { newListings ->
                _uiState.update { current ->
                    current.copy(
                        listings = if (reset) newListings
                        else (current.listings + newListings).distinctBy { it.id },
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
                        error = cause.message,
                    )
                }
            }
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
        _uiState.update { it.copy(error = null) }
    }
}