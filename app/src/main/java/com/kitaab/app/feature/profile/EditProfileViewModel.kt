package com.kitaab.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitaab.app.domain.model.UserProfile
import com.kitaab.app.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

data class EditProfileUiState(
    val name: String = "",
    val city: String = "",
    val pincode: String = "",
    val selectedExamTags: Set<String> = emptySet(),
    val nameError: String? = null,
    val cityError: String? = null,
    val pincodeError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

sealed interface EditProfileEvent {
    data object SaveSuccess : EditProfileEvent
}

@Serializable
private data class ProfileEdit(
    val name: String,
    val city: String,
    val pincode: String,
    @SerialName("exam_tags") val examTags: List<String>,
)

@HiltViewModel
class EditProfileViewModel
    @Inject
    constructor(
        private val supabase: SupabaseClient,
        private val userPrefs: UserPreferencesRepository,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(EditProfileUiState())
        val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

        private val _events = Channel<EditProfileEvent>(Channel.BUFFERED)
        val events = _events.receiveAsFlow()

        init {
            loadCurrentProfile()
        }

        private fun loadCurrentProfile() {
            viewModelScope.launch {
                val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                runCatching {
                    supabase.postgrest["users"]
                        .select { filter { eq("id", userId) } }
                        .decodeList<UserProfile>()
                        .firstOrNull()
                }.onSuccess { profile ->
                    if (profile != null) {
                        _uiState.update {
                            it.copy(
                                name = profile.name,
                                city = profile.city ?: "",
                                pincode = profile.pincode ?: "",
                                selectedExamTags = profile.examTags.toSet(),
                            )
                        }
                    }
                }
            }
        }

        fun onNameChanged(value: String) {
            _uiState.update { it.copy(name = value, nameError = null) }
        }

        fun onCityChanged(value: String) {
            _uiState.update { it.copy(city = value, cityError = null) }
        }

        fun onPincodeChanged(value: String) {
            if (value.length <= 6 && value.all { it.isDigit() }) {
                _uiState.update { it.copy(pincode = value, pincodeError = null) }
            }
        }

        fun onExamTagToggled(tag: String) {
            _uiState.update { state ->
                val updated = state.selectedExamTags.toMutableSet()
                if (tag in updated) updated.remove(tag) else updated.add(tag)
                state.copy(selectedExamTags = updated)
            }
        }

        fun clearError() {
            _uiState.update { it.copy(error = null) }
        }

        fun saveProfile() {
            if (!validate()) return
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, error = null) }
                val userId = supabase.auth.currentUserOrNull()?.id
                if (userId == null) {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Session expired. Please sign in again.")
                    }
                    return@launch
                }
                val state = _uiState.value
                runCatching {
                    supabase.postgrest["users"].update(
                        ProfileEdit(
                            name = state.name.trim(),
                            city = state.city.trim(),
                            pincode = state.pincode.trim(),
                            examTags = state.selectedExamTags.toList(),
                        ),
                    ) {
                        filter { eq("id", userId) }
                    }
                    userPrefs.setLocation(
                        city = state.city.trim(),
                        pincode = state.pincode.trim(),
                    )
                }.fold(
                    onSuccess = {
                        _uiState.update { it.copy(isLoading = false) }
                        _events.send(EditProfileEvent.SaveSuccess)
                    },
                    onFailure = { cause ->
                        val message =
                            when (cause) {
                                is RestException -> "Failed to save profile. Please try again."
                                is HttpRequestException -> "No internet connection. Please try again."
                                else -> cause.message ?: "Something went wrong."
                            }
                        _uiState.update { it.copy(isLoading = false, error = message) }
                    },
                )
            }
        }

        private fun validate(): Boolean {
            var valid = true
            val state = _uiState.value
            if (state.name.isBlank()) {
                _uiState.update { it.copy(nameError = "Name is required") }
                valid = false
            } else if (state.name.trim().length < 2) {
                _uiState.update { it.copy(nameError = "Name must be at least 2 characters") }
                valid = false
            }
            if (state.city.isBlank()) {
                _uiState.update { it.copy(cityError = "City is required") }
                valid = false
            }
            if (state.pincode.isBlank()) {
                _uiState.update { it.copy(pincodeError = "Pincode is required") }
                valid = false
            } else if (state.pincode.length != 6) {
                _uiState.update { it.copy(pincodeError = "Enter a valid 6-digit pincode") }
                valid = false
            }
            return valid
        }
    }
