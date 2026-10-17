package com.kitaab.app.feature.listing

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitaab.app.domain.model.Listing
import com.kitaab.app.domain.model.UserProfile
import com.kitaab.app.domain.repository.ConversationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListingDetailViewModel
    @Inject
    constructor(
        private val supabase: SupabaseClient,
        private val conversationRepository: ConversationRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val listingId: String = checkNotNull(savedStateHandle["listingId"])

        private val _uiState = MutableStateFlow(ListingDetailUiState())
        val uiState = _uiState.asStateFlow()

        private val _events = Channel<ListingDetailEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        private var chatJob: kotlinx.coroutines.Job? = null
        private var loadGeneration = 0

        private val referrerId: String? = savedStateHandle["referrerId"]

        init {
            load()
        }

        /**
         * Load the listing first so optional seller/recommendation requests cannot delay
         * the main content. A generation token prevents an older retry from overwriting
         * a newer result.
         */
        fun load() {
            val generation = ++loadGeneration

            viewModelScope.launch {
                _uiState.update {
                    it.copy(
                        isLoading = it.listing == null,
                        error = null,
                    )
                }

                try {
                    val currentUserId = supabase.auth.currentSessionOrNull()?.user?.id
                    val listing =
                        supabase.postgrest["listings"]
                            .select { filter { eq("id", listingId) } }
                            .decodeList<Listing>()
                            .firstOrNull()
                            ?: error("Listing not found")

                    if (generation != loadGeneration) return@launch

                    _uiState.update {
                        it.copy(
                            listing = listing,
                            isLoading = false,
                            isOwnListing = currentUserId == listing.sellerId,
                            seller = null,
                            similarListings = emptyList(),
                        )
                    }

                    // These requests are independent and run after the listing is visible.
                    launch { loadSeller(listing.sellerId, generation) }
                    launch { loadSimilarListings(listing, generation) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (cause: Exception) {
                    if (generation == loadGeneration) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = cause.message ?: "Failed to load listing",
                            )
                        }
                    }
                }
            }
        }

        private suspend fun loadSeller(
            sellerId: String,
            generation: Int,
        ) {
            try {
                val seller =
                    supabase.postgrest["users"]
                        .select { filter { eq("id", sellerId) } }
                        .decodeList<UserProfile>()
                        .firstOrNull()

                if (generation == loadGeneration) {
                    _uiState.update { it.copy(seller = seller) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Seller details are optional; keep the listing usable if this request fails.
            }
        }

        private suspend fun loadSimilarListings(
            listing: Listing,
            generation: Int,
        ) {
            // Without a city, "nearby" recommendations would be misleading.
            val city = listing.city?.takeIf { it.isNotBlank() } ?: return
            val excludeIds =
                listOfNotNull(listing.id, referrerId)
                    .joinToString(",") { "\"$it\"" }

            try {
                val similarListings =
                    supabase.postgrest["listings"]
                        .select {
                            filter {
                                eq("status", "ACTIVE")
                                eq("city", city)
                                filterNot("id", FilterOperator.IN, "($excludeIds)")
                            }
                            order("created_at", order = Order.DESCENDING)
                            limit(6)
                        }
                        .decodeList<Listing>()

                if (generation == loadGeneration) {
                    _uiState.update { it.copy(similarListings = similarListings) }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // Recommendations are non-critical; the main listing remains available.
            }
        }

        fun onMessageSellerClick() {
            val listing = _uiState.value.listing ?: return
            if (chatJob?.isActive == true) return

            chatJob =
                viewModelScope.launch {
                    _uiState.update { it.copy(isChatLoading = true) }
                    conversationRepository.getOrCreateConversation(
                        listingId = listing.id,
                        sellerId = listing.sellerId,
                    ).onSuccess { conversation ->
                        _uiState.update { it.copy(isChatLoading = false) }
                        _events.send(ListingDetailEvent.NavigateToChat(conversationId = conversation.id))
                    }.onFailure { cause ->
                        _uiState.update {
                            it.copy(
                                isChatLoading = false,
                                error = cause.message ?: "Could not open chat",
                            )
                        }
                    }
                }
        }

        fun onSeeAllRequestsClick() {
            viewModelScope.launch {
                _events.send(ListingDetailEvent.NavigateToDonationRequests(listingId))
            }
        }

        fun onRequestDonationClick() {
            viewModelScope.launch {
                _events.send(ListingDetailEvent.NavigateToDonationRequest(listingId = listingId))
            }
        }

        fun clearError() {
            _uiState.update { it.copy(error = null) }
        }
    }
