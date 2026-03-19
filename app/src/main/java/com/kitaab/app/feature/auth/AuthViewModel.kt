package com.kitaab.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitaab.app.data.remote.supabase
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class UserRow(
    val id: String,
    val name: String,
    val email: String,
)

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
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
    val isSuccess: Boolean = false,
)

class AuthViewModel : ViewModel() {
    private val _loginState = MutableStateFlow(LoginUiState())
    val loginState = _loginState.asStateFlow()

    private val _signUpState = MutableStateFlow(SignUpUiState())
    val signUpState = _signUpState.asStateFlow()

    // ── Login ────────────────────────────────────────────────────────────────

    fun onLoginEmailChanged(value: String) {
        _loginState.update { it.copy(email = value, emailError = null) }
    }

    fun onLoginPasswordChanged(value: String) {
        _loginState.update { it.copy(password = value, passwordError = null) }
    }

    fun signInWithEmail() {
        if (!validateLoginInputs()) return
        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, error = null) }
            try {
                supabase.auth.signInWith(Email) {
                    email = _loginState.value.email.trim()
                    password = _loginState.value.password
                }
                _loginState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _loginState.update {
                    it.copy(isLoading = false, error = e.message ?: "Sign in failed")
                }
            }
        }
    }

    fun signInWithGoogle() {
        viewModelScope.launch {
            _loginState.update { it.copy(isLoading = true, error = null) }
            try {
                supabase.auth.signInWith(Google)
                _loginState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _loginState.update {
                    it.copy(isLoading = false, error = e.message ?: "Google sign in failed")
                }
            }
        }
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
        if (!validateSignUpInputs()) return
        viewModelScope.launch {
            _signUpState.update { it.copy(isLoading = true, error = null) }
            try {
                supabase.auth.signUpWith(Email) {
                    email = _signUpState.value.email.trim()
                    password = _signUpState.value.password
                }
                val userId = supabase.auth.currentUserOrNull()?.id ?: return@launch
                supabase.postgrest["users"].insert(
                    UserRow(
                        id = userId,
                        email = _signUpState.value.email.trim(),
                        name = "",
                    ),
                )
                _signUpState.update { it.copy(isLoading = false, isSuccess = true) }
            } catch (e: Exception) {
                _signUpState.update {
                    it.copy(isLoading = false, error = e.message ?: "Sign up failed")
                }
            }
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
}
