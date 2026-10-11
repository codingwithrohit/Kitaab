package com.kitaab.app.feature.donation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitaab.app.domain.repository.DonationRequestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DonationRequestsSellerViewModel
    @Inject
    constructor(
        private val donationRequestRepository: DonationRequestRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DonationRequestsSellerUiState())
        val uiState: StateFlow<DonationRequestsSellerUiState> = _uiState.asStateFlow()

        private val _events = Channel<DonationRequestsSellerEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        fun loadRequests(listingId: String) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                donationRequestRepository.getRequestsForListing(listingId)
                    .onSuccess { requests ->
                        _uiState.update { it.copy(requests = requests, isLoading = false) }
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = throwable.message ?: "Failed to load requests",
                            )
                        }
                    }
            }
        }

        fun acceptRequest(
            requestId: String,
            listingId: String,
        ) {
            viewModelScope.launch {
                _uiState.update { it.copy(isAccepting = requestId, error = null) }
                donationRequestRepository.acceptRequest(requestId, listingId)
                    .onSuccess {
                        _uiState.update { state ->
                            state.copy(
                                isAccepting = null,
                                acceptedRequestId = requestId,
                                requests =
                                    state.requests.map { item ->
                                        if (item.request.id == requestId) {
                                            item.copy(request = item.request.copy(status = "ACCEPTED"))
                                        } else {
                                            item.copy(request = item.request.copy(status = "REJECTED"))
                                        }
                                    },
                            )
                        }
                        _events.send(DonationRequestsSellerEvent.AcceptSuccess)
                    }
                    .onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isAccepting = null,
                                error = throwable.message ?: "Failed to accept request",
                            )
                        }
                    }
            }
        }

        fun clearError() {
            _uiState.update { it.copy(error = null) }
        }
    }
