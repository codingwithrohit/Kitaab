package com.kitaab.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kitaab.app.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

sealed interface SplashDestination {
    data object Home : SplashDestination
    data object ProfileSetup : SplashDestination
    data object Onboarding : SplashDestination
}

@Serializable
private data class UserProfileCheckRow(
    @SerialName("profile_complete") val profileComplete: Boolean = false,
)
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val userPrefs: UserPreferencesRepository,
) : ViewModel() {

    private val _destination = Channel<SplashDestination>(Channel.BUFFERED)
    val destination = _destination.receiveAsFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            // Guarantee minimum splash display time in parallel with session check
            launch {
                delay(1200)
                _isReady.value = true
            }

            supabase.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        // Supabase is source of truth — DataStore is just a local cache
                        val profileComplete = runCatching {
                            val userId = supabase.auth.currentSessionOrNull()?.user?.id
                                ?: return@collect
                            supabase.postgrest["users"]
                                .select { filter { eq("id", userId) } }
                                .decodeSingle<UserProfileCheckRow>()
                                .profileComplete
                        }.getOrDefault(false)

                        // Sync DataStore cache to match Supabase
                        userPrefs.setProfileComplete(profileComplete)

                        _isReady.value = true
                        if (profileComplete) {
                            _destination.send(SplashDestination.Home)
                        } else {
                            _destination.send(SplashDestination.ProfileSetup)
                        }
                        return@collect
                    }
                    is SessionStatus.NotAuthenticated -> {
                        _isReady.value = true
                        _destination.send(SplashDestination.Onboarding)
                        return@collect
                    }
                    is SessionStatus.Initializing -> Unit
                    is SessionStatus.RefreshFailure -> {
                        // Local session exists, network failed — go to Home
                        // and let Supabase retry token refresh in background
                        val profileComplete = userPrefs.isProfileComplete.first()
                        _isReady.value = true
                        if (profileComplete) {
                            _destination.send(SplashDestination.Home)
                        } else {
                            _destination.send(SplashDestination.ProfileSetup)
                        }
                        return@collect
                    }
                }
            }
        }
    }
}