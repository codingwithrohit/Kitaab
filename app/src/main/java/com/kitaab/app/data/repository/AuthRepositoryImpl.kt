package com.kitaab.app.data.repository

import com.kitaab.app.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class UserProfileRow(
    @SerialName("profile_complete") val profileComplete: Boolean = false,
)

@Singleton
class AuthRepositoryImpl
    @Inject
    constructor(
        private val supabase: SupabaseClient,
    ) : AuthRepository {
        override suspend fun signIn(
            email: String,
            password: String,
        ): Result<Unit> =
            runCatching {
                supabase.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
            }.mapError()

        override suspend fun signUp(
            email: String,
            password: String,
        ): Result<Unit> =
            runCatching {
                supabase.auth.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }

                val user =
                    supabase.auth.currentSessionOrNull()?.user
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
                    ),
                )

                Unit
            }.mapError()

        override suspend fun signInWithGoogle(idToken: String): Result<Unit> =
            runCatching {
                // Exchange the Google ID token with Supabase.
                // Supabase creates or retrieves the user, then returns a session.
                supabase.auth.signInWith(IDToken) {
                    this.idToken = idToken
                    this.provider = Google
                }

                // Insert a users row only if this is a brand-new account.
                // Supabase does not insert into public.users automatically.
                val user =
                    supabase.auth.currentSessionOrNull()?.user
                        ?: error("Session not available after Google sign-in.")

                // Check if the user row already exists to avoid duplicate insert errors.
                val existing =
                    runCatching {
                        supabase.postgrest["users"]
                            .select { filter { eq("id", user.id) } }
                    }.getOrNull()

                val rowExists =
                    existing != null &&
                        existing.data.trimIndent().length > 2 // "[]" means empty

                if (!rowExists) {
                    supabase.postgrest["users"].insert(
                        mapOf(
                            "id" to user.id,
                            "email" to (user.email ?: ""),
                            "name" to (
                                user.userMetadata?.get("full_name")?.toString()
                                    ?.trim('"') ?: ""
                            ),
                        ),
                    )
                }

                Unit
            }.mapError()

        override suspend fun isProfileComplete(): Result<Boolean> =
            runCatching {
                val userId =
                    supabase.auth.currentSessionOrNull()?.user?.id
                        ?: return Result.success(false)
                val result =
                    supabase.postgrest["users"]
                        .select {
                            filter { eq("id", userId) }
                        }
                        .decodeSingle<UserProfileRow>()
                result.profileComplete
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

        private fun <T> Result<T>.mapError(): Result<T> =
            recoverCatching { cause ->
                val message =
                    when (cause) {
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
