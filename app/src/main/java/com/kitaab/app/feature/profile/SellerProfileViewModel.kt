package com.kitaab.app.feature.profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitaab.app.domain.model.Listing
import com.kitaab.app.domain.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SellerProfileViewModel
    @Inject
    constructor(
        private val supabase: SupabaseClient,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val userId: String = checkNotNull(savedStateHandle["userId"])

        private val _uiState = MutableStateFlow(SellerProfileUiState())
        val uiState: StateFlow<SellerProfileUiState> = _uiState.asStateFlow()

        init {
            load()
        }

        fun load() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                runCatching {
                    val profile =
                        supabase.postgrest["users"]
                            .select { filter { eq("id", userId) } }
                            .decodeList<UserProfile>()
                            .firstOrNull()

                    val listings =
                        supabase.postgrest["listings"]
                            .select {
                                filter {
                                    eq("seller_id", userId)
                                    eq("status", "ACTIVE")
                                }
                                order("created_at", order = Order.DESCENDING)
                            }
                            .decodeList<Listing>()

                    _uiState.update {
                        it.copy(
                            profile = profile,
                            listings = listings,
                            isLoading = false,
                        )
                    }
                }.onFailure { cause ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = cause.message ?: "Failed to load profile",
                        )
                    }
                }
            }
        }

        fun clearError() {
            _uiState.update { it.copy(error = null) }
        }
    }
