package com.kitaab.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitaab.app.data.local.db.ListingDao
import com.kitaab.app.data.local.db.UserProfileDao
import com.kitaab.app.data.local.db.toCached
import com.kitaab.app.data.local.db.toDomain
import com.kitaab.app.domain.model.DonationRequest
import com.kitaab.app.domain.model.Listing
import com.kitaab.app.domain.model.UserProfile
import com.kitaab.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ProfileTab { LISTINGS, REQUESTS }

@HiltViewModel
class ProfileViewModel
    @Inject
    constructor(
        private val supabase: SupabaseClient,
        private val authRepository: AuthRepository,
        private val userProfileDao: UserProfileDao,
        private val listingDao: ListingDao,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(ProfileUiState())
        val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

        private val _events = Channel<ProfileEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        init {
            loadFromCache()
            load(showFullLoader = false)
        }

        private fun loadFromCache() {
            viewModelScope.launch {
                val cachedProfile = userProfileDao.get()?.toDomain()
                val cachedListings = listingDao.getAll().map { it.toDomain() }
                if (cachedProfile != null) {
                    _uiState.update {
                        it.copy(
                            profile = cachedProfile,
                            ownListings = cachedListings,
                        )
                    }
                }
            }
        }

        fun load(showFullLoader: Boolean = true) {
            viewModelScope.launch {
                val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                _uiState.update {
                    if (showFullLoader) {
                        it.copy(isLoading = true, error = null)
                    } else {
                        it.copy(isRefreshing = true, error = null)
                    }
                }

                runCatching {
                    val profile =
                        supabase.postgrest["users"]
                            .select { filter { eq("id", userId) } }
                            .decodeList<UserProfile>()
                            .firstOrNull()

                    val listings =
                        supabase.postgrest["listings"]
                            .select {
                                filter { eq("seller_id", userId) }
                                order("created_at", order = Order.DESCENDING)
                            }
                            .decodeList<Listing>()

                    val requests =
                        supabase.postgrest["donation_requests"]
                            .select {
                                filter { eq("requester_id", userId) }
                                order("created_at", order = Order.DESCENDING)
                            }
                            .decodeList<DonationRequest>()

                    // Update cache
                    if (profile != null) userProfileDao.upsert(profile.toCached())
                    listingDao.upsertAll(listings.map { it.toCached() })

                    _uiState.update {
                        it.copy(
                            profile = profile,
                            ownListings = listings,
                            myRequests = requests,
                            isLoading = false,
                            isRefreshing = false,
                        )
                    }
                }.onFailure { cause ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = cause.message ?: "Failed to load profile",
                        )
                    }
                }
            }
        }

        fun onTabSelected(tab: ProfileTab) {
            _uiState.update { it.copy(selectedTab = tab) }
        }

        fun pauseListing(listingId: String) {
            updateListingStatus(listingId, "PAUSED")
        }

        fun reactivateListing(listingId: String) {
            updateListingStatus(listingId, "ACTIVE")
        }

        fun markListingSold(listingId: String) {
            updateListingStatus(listingId, "COMPLETED")
        }

        private fun updateListingStatus(
            listingId: String,
            status: String,
        ) {
            viewModelScope.launch {
                runCatching {
                    supabase.postgrest["listings"]
                        .update(mapOf("status" to status)) {
                            filter { eq("id", listingId) }
                        }
                    // Update both cache and state
                    listingDao.updateStatus(listingId, status)
                    _uiState.update { state ->
                        state.copy(
                            ownListings =
                                state.ownListings.map { listing ->
                                    if (listing.id == listingId) {
                                        listing.copy(status = status)
                                    } else {
                                        listing
                                    }
                                },
                        )
                    }
                }.onFailure { cause ->
                    _uiState.update {
                        it.copy(error = cause.message ?: "Failed to update listing")
                    }
                }
            }
        }

        fun signOut() {
            viewModelScope.launch {
                _uiState.update { it.copy(isSigningOut = true) }
                // Clear cache on sign out
                userProfileDao.clear()
                listingDao.clear()
                authRepository.signOut()
                    .onSuccess { _events.send(ProfileEvent.SignedOut) }
                    .onFailure { cause ->
                        _uiState.update {
                            it.copy(
                                isSigningOut = false,
                                error = cause.message ?: "Failed to sign out",
                            )
                        }
                    }
            }
        }

        fun deleteAccount() {
            viewModelScope.launch {
                _uiState.update { it.copy(isDeletingAccount = true) }
                userProfileDao.clear()
                listingDao.clear()
                authRepository.deleteAccount()
                    .onSuccess { _events.send(ProfileEvent.AccountDeleted) }
                    .onFailure { cause ->
                        _uiState.update {
                            it.copy(
                                isDeletingAccount = false,
                                error = cause.message ?: "Failed to delete account",
                            )
                        }
                    }
            }
        }

        fun clearError() {
            _uiState.update { it.copy(error = null) }
        }
    }
