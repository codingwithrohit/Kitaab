package com.kitaab.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitaab.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ── UiState definitions ──────────────────────────────────────────────────────

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class SignUpUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

data class DeleteAccountUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
)

// ── One-shot navigation events ───────────────────────────────────────────────
// Using Channel instead of isSuccess: Boolean in UiState prevents the event
// from firing again on recomposition, rotation, or back-stack restore.

sealed interface AuthEvent {
    data object LoginSuccess : AuthEvent
    data object SignUpSuccess : AuthEvent
    data object DeleteAccountSuccess : AuthEvent
}

// ── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState = _loginState.asStateFlow()

    private val _signUpState = MutableStateFlow(SignUpUiState())
    val signUpState = _signUpState.asStateFlow()

    private val _deleteAccountState = MutableStateFlow(DeleteAccountUiState())
    val deleteAccountState = _deleteAccountState.asStateFlow()

    // Buffered so events are not dropped if the screen is not yet collecting
    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // In-flight guards — prevents double-tap from firing two network calls
    private var loginJob: Job? = null
    private var signUpJob: Job? = null
    private var deleteJob: Job? = null

    // ── Login ────────────────────────────────────────────────────────────────

    fun onLoginEmailChanged(value: String) {
        _loginState.update { it.copy(email = value, emailError = null) }
    }

    fun onLoginPasswordChanged(value: String) {
        _loginState.update { it.copy(password = value, passwordError = null) }
    }

    fun signInWithEmail() {
        if (loginJob?.isActive == true) return
        if (!validateLoginInputs()) return

        loginJob = viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, error = null) }

            authRepository.signIn(
                email = _loginState.value.email.trim(),
                password = _loginState.value.password,
            ).fold(
                onSuccess = {
                    _loginState.update { it.copy(isLoading = false) }
                    _events.send(AuthEvent.LoginSuccess)
                },
                onFailure = { cause ->
                    _loginState.update { it.copy(isLoading = false, error = cause.message) }
                },
            )
        }
    }

    // Google Sign-In — wired in Phase 2 with Credential Manager + OAuth redirect.
    // Intentionally left as a no-op stub so the UI can keep the button hidden.
    fun signInWithGoogle() {
        // TODO Phase 2: implement with CredentialManager + Supabase OAuth redirect
    }

    fun clearLoginError() {
        _loginState.update { it.copy(error = null) }
    }

    private fun validateLoginInputs(): Boolean {
        var valid = true
        val email = _loginState.value.email.trim()
        val password = _loginState.value.password

        if (email.isBlank()) {
            _loginState.update { it.copy(emailError = "Email is required") }
            valid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _loginState.update { it.copy(emailError = "Enter a valid email") }
            valid = false
        }

        if (password.isBlank()) {
            _loginState.update { it.copy(passwordError = "Password is required") }
            valid = false
        } else if (password.length < 6) {
            _loginState.update { it.copy(passwordError = "Password must be at least 6 characters") }
            valid = false
        }

        return valid
    }

    // ── Sign up ──────────────────────────────────────────────────────────────

    fun onSignUpEmailChanged(value: String) {
        _signUpState.update { it.copy(email = value, emailError = null) }
    }

    fun onSignUpPasswordChanged(value: String) {
        _signUpState.update { it.copy(password = value, passwordError = null) }
    }

    fun onConfirmPasswordChanged(value: String) {
        _signUpState.update { it.copy(confirmPassword = value, confirmPasswordError = null) }
    }

    fun signUpWithEmail() {
        if (signUpJob?.isActive == true) return
        if (!validateSignUpInputs()) return

        signUpJob = viewModelScope.launch {
            _signUpState.update { it.copy(isLoading = true, error = null) }

            authRepository.signUp(
                email = _signUpState.value.email.trim(),
                password = _signUpState.value.password,
            ).fold(
                onSuccess = {
                    _signUpState.update { it.copy(isLoading = false) }
                    _events.send(AuthEvent.SignUpSuccess)
                },
                onFailure = { cause ->
                    _signUpState.update { it.copy(isLoading = false, error = cause.message) }
                },
            )
        }
    }

    fun clearSignUpError() {
        _signUpState.update { it.copy(error = null) }
    }

    private fun validateSignUpInputs(): Boolean {
        var valid = true
        val email = _signUpState.value.email.trim()
        val password = _signUpState.value.password
        val confirmPassword = _signUpState.value.confirmPassword

        if (email.isBlank()) {
            _signUpState.update { it.copy(emailError = "Email is required") }
            valid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _signUpState.update { it.copy(emailError = "Enter a valid email") }
            valid = false
        }

        if (password.isBlank()) {
            _signUpState.update { it.copy(passwordError = "Password is required") }
            valid = false
        } else if (password.length < 6) {
            _signUpState.update { it.copy(passwordError = "Password must be at least 6 characters") }
            valid = false
        }

        if (confirmPassword != password) {
            _signUpState.update { it.copy(confirmPasswordError = "Passwords do not match") }
            valid = false
        }

        return valid
    }

    // ── Delete account ───────────────────────────────────────────────────────

    fun deleteAccount() {
        if (deleteJob?.isActive == true) return

        deleteJob = viewModelScope.launch {
            _deleteAccountState.update { it.copy(isLoading = true, error = null) }

            authRepository.deleteAccount().fold(
                onSuccess = {
                    _deleteAccountState.update { it.copy(isLoading = false) }
                    _events.send(AuthEvent.DeleteAccountSuccess)
                },
                onFailure = { cause ->
                    _deleteAccountState.update {
                        it.copy(isLoading = false, error = cause.message)
                    }
                },
            )
        }
    }

    fun clearDeleteAccountError() {
        _deleteAccountState.update { it.copy(error = null) }
    }
}