package com.kitaab.app.data.repository

import com.kitaab.app.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): Result<Unit> =
        runCatching {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            // signInWith returns Unit — runCatching correctly infers Result<Unit>
        }.mapError()

    override suspend fun signUp(email: String, password: String): Result<Unit> =
        runCatching {
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            // Session is available immediately when email confirmation is OFF.
            // Use currentSessionOrNull()?.user — NOT currentUserOrNull() which can
            // return null before the session propagates internally.
            val user = supabase.auth.currentSessionOrNull()?.user
                ?: error(
                    "Session not available after signup. " +
                            "Go to Supabase → Auth → Providers → Email and disable " +
                            "'Confirm email', 'Secure email change', and 'Secure password change'.",
                )

            supabase.postgrest["users"].insert(
                mapOf(
                    "id" to user.id,
                    "email" to (user.email ?: email),
                    "name" to "",
                )
            )

            // Explicit Unit ensures runCatching<Unit> regardless of insert()'s return type
            Unit
        }.mapError()

    override suspend fun signOut(): Result<Unit> =
        runCatching {
            supabase.auth.signOut()
        }.mapError()

    override suspend fun deleteAccount(): Result<Unit> =
        runCatching {
            supabase.postgrest.rpc("delete_user")
            supabase.auth.signOut()
            Unit
        }.mapError()

    // Maps Supabase-specific exceptions to clean user-facing messages.
    // Never let raw Ktor or Supabase stack traces reach the UI.
    private fun <T> Result<T>.mapError(): Result<T> =
        recoverCatching { cause ->
            val message = when (cause) {
                is RestException -> mapRestError(cause)
                is HttpRequestException -> "No internet connection. Please try again."
                else -> cause.message ?: "Something went wrong. Please try again."
            }
            throw Exception(message)
        }

    private fun mapRestError(e: RestException): String =
        when {
            e.error?.contains("invalid_credentials", ignoreCase = true) == true ->
                "Wrong email or password."
            e.error?.contains("email_address_invalid", ignoreCase = true) == true ->
                "Enter a valid email address."
            e.error?.contains("user_already_exists", ignoreCase = true) == true ||
                    e.message?.contains("already registered", ignoreCase = true) == true ->
                "An account with this email already exists. Try signing in."
            e.error?.contains("weak_password", ignoreCase = true) == true ->
                "Password is too weak. Use at least 6 characters."
            e.error?.contains("over_email_send_rate_limit", ignoreCase = true) == true ||
                    e.message?.contains("rate limit", ignoreCase = true) == true ->
                "Too many attempts. Please wait a few minutes and try again."
            e.error?.contains("email_not_confirmed", ignoreCase = true) == true ->
                "Please confirm your email before signing in."
            else -> "Something went wrong. Please try again."
        }
}