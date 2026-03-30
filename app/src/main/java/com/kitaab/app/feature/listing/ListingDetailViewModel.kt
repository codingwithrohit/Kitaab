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

        private val referrerId: String? = savedStateHandle["referrerId"]

        init {
            load()
        }

        fun load() {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }

                runCatching {
                    val currentUserId = supabase.auth.currentSessionOrNull()?.user?.id

                    val listing =
                        supabase.postgrest["listings"]
                            .select { filter { eq("id", listingId) } }
                            .decodeList<Listing>()
                            .firstOrNull()
                            ?: error("Listing not found")

                    val seller =
                        runCatching {
                            supabase.postgrest["users"]
                                .select { filter { eq("id", listing.sellerId) } }
                                .decodeList<UserProfile>()
                                .firstOrNull()
                        }.getOrNull()

                    val excludeIds =
                        listOfNotNull(listingId, referrerId)
                            .joinToString(",") { "\"$it\"" }

                    val similarListings =
                        runCatching {
                            supabase.postgrest["listings"]
                                .select {
                                    filter {
                                        eq("status", "ACTIVE")
                                        if (listing.city != null) eq("city", listing.city)
                                        filterNot("id", FilterOperator.IN, "($excludeIds)")
                                    }
                                    order("created_at", order = Order.DESCENDING)
                                    limit(6)
                                }
                                .decodeList<Listing>()
                        }.getOrDefault(emptyList())

                    _uiState.update {
                        it.copy(
                            listing = listing,
                            seller = seller,
                            similarListings = similarListings,
                            isLoading = false,
                            isOwnListing = currentUserId == listing.sellerId,
                        )
                    }
                }.onFailure { cause ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = cause.message ?: "Failed to load listing",
                        )
                    }
                }
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
