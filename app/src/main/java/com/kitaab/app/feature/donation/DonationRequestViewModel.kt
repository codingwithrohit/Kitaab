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

val examTags = listOf("JEE", "NEET", "UPSC", "CAT", "GATE", "College", "Other")

@HiltViewModel
class DonationRequestViewModel
    @Inject
    constructor(
        private val donationRequestRepository: DonationRequestRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(DonationRequestUiState())
        val uiState: StateFlow<DonationRequestUiState> = _uiState.asStateFlow()

        private val _events = Channel<DonationRequestEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        private var submitJob: kotlinx.coroutines.Job? = null

        fun onReasonChanged(value: String) {
            if (value.length <= 150) {
                _uiState.update { it.copy(reason = value, reasonError = null) }
            }
        }

        fun onExamTagSelected(tag: String) {
            _uiState.update { state ->
                state.copy(selectedExamTag = if (state.selectedExamTag == tag) null else tag)
            }
        }

        fun clearError() {
            _uiState.update { it.copy(error = null) }
        }

        fun submitRequest(listingId: String) {
            val reason = _uiState.value.reason.trim()
            if (reason.isBlank()) {
                _uiState.update { it.copy(reasonError = "Please tell the seller why you need this book") }
                return
            }
            if (reason.length < 20) {
                _uiState.update { it.copy(reasonError = "Please write at least 20 characters") }
                return
            }
            if (submitJob?.isActive == true) return

            submitJob =
                viewModelScope.launch {
                    _uiState.update { it.copy(isSubmitting = true, error = null) }
                    donationRequestRepository.submitRequest(
                        listingId = listingId,
                        reason = reason,
                        examTag = _uiState.value.selectedExamTag,
                    ).onSuccess {
                        _uiState.update { it.copy(isSubmitting = false) }
                        _events.send(DonationRequestEvent.SubmitSuccess)
                    }.onFailure { throwable ->
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                error = throwable.message ?: "Failed to submit request",
                            )
                        }
                    }
                }
        }
    }
