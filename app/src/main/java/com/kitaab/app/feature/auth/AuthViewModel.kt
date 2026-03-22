package com.kitaab.app.feature.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.kitaab.app.BuildConfig
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

// ── UiState ──────────────────────────────────────────────────────────────────

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
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

// ── One-shot navigation events ────────────────────────────────────────────────

sealed interface AuthEvent {
    data object LoginSuccess : AuthEvent
    data object SignUpSuccess : AuthEvent
    data object SignOutSuccess : AuthEvent
    data object DeleteAccountSuccess : AuthEvent
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

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

    private val _events = Channel<AuthEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var loginJob: Job? = null
    private var signUpJob: Job? = null
    private var googleJob: Job? = null
    private var signOutJob: Job? = null
    private var deleteJob: Job? = null

    // ── Email sign-in ─────────────────────────────────────────────────────────

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

    fun clearLoginError() {
        _loginState.update { it.copy(error = null) }
    }

    // ── Google sign-in ────────────────────────────────────────────────────────

    fun signInWithGoogle(context: Context) {
        if (googleJob?.isActive == true) return

        googleJob = viewModelScope.launch {
            _loginState.update { it.copy(isGoogleLoading = true, error = null) }

            try {
                // Build the Google ID token request
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false) // show all accounts, not just prev used
                    .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false) // don't auto-pick — let user choose
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val credentialManager = CredentialManager.create(context)
                val result = credentialManager.getCredential(
                    request = request,
                    context = context,
                )

                // Extract the Google ID token from the credential response
                val googleIdTokenCredential = GoogleIdTokenCredential
                    .createFrom(result.credential.data)
                val idToken = googleIdTokenCredential.idToken

                // Exchange the token with Supabase
                authRepository.signInWithGoogle(idToken).fold(
                    onSuccess = {
                        _loginState.update { it.copy(isGoogleLoading = false) }
                        _events.send(AuthEvent.LoginSuccess)
                    },
                    onFailure = { cause ->
                        _loginState.update {
                            it.copy(isGoogleLoading = false, error = cause.message)
                        }
                    },
                )
            } catch (e: GetCredentialCancellationException) {
                // User dismissed the picker — not an error, just reset loading
                _loginState.update { it.copy(isGoogleLoading = false) }
            } catch (e: Exception) {
                _loginState.update {
                    it.copy(
                        isGoogleLoading = false,
                        error = when {
                            e.message?.contains("No credentials available", ignoreCase = true) == true ->
                                "No Google account found on this device."
                            e.message?.contains("ApiException: 10", ignoreCase = true) == true ->
                                "Google Sign-In configuration error. Check SHA-1 in Google Cloud Console."
                            else -> "Google Sign-In failed. Please try again."
                        },
                    )
                }
            }
        }
    }

    // ── Sign up ───────────────────────────────────────────────────────────────

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

    // ── Sign out ──────────────────────────────────────────────────────────────

    fun signOut() {
        if (signOutJob?.isActive == true) return

        signOutJob = viewModelScope.launch {
            authRepository.signOut().fold(
                onSuccess = {
                    _events.send(AuthEvent.SignOutSuccess)
                },
                onFailure = { cause ->
                    // Sign-out failure is rare — surface it on the login error field
                    // since the user will land on login anyway
                    _loginState.update { it.copy(error = cause.message) }
                },
            )
        }
    }

    // ── Delete account ────────────────────────────────────────────────────────

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

    // ── Validation ────────────────────────────────────────────────────────────

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
}