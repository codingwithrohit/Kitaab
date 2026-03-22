package com.kitaab.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface SplashDestination {
    data object Home : SplashDestination
    data object Onboarding : SplashDestination
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val supabase: SupabaseClient,
) : ViewModel() {

    private val _destination = Channel<SplashDestination>(Channel.BUFFERED)
    val destination = _destination.receiveAsFlow()

    // Exposed so MainActivity can hold the system splash screen until
    // our custom SplashScreen composable is ready to take over.
    private val _isReady = MutableStateFlow(false)
    val isReady = _isReady.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            // Guarantee the custom splash is visible for at least 1200ms.
            // This runs in parallel with the session check below.
            launch {
                delay(1200)
                _isReady.value = true
            }

            supabase.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        // Wait until the minimum display time has passed before navigating
                        _isReady.value = true
                        _destination.send(SplashDestination.Home)
                        return@collect
                    }
                    is SessionStatus.NotAuthenticated -> {
                        _isReady.value = true
                        _destination.send(SplashDestination.Onboarding)
                        return@collect
                    }
                    // Still reading session from disk — stay on splash
                    is SessionStatus.Initializing -> Unit
                    // Network error but local session may exist — go to Home
                    is SessionStatus.RefreshFailure -> {
                        _isReady.value = true
                        _destination.send(SplashDestination.Home)
                        return@collect
                    }
                }
            }
        }
    }
}