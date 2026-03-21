package com.kitaab.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.channels.Channel
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

    init {
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            supabase.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        _destination.send(SplashDestination.Home)
                        return@collect
                    }

                    is SessionStatus.NotAuthenticated -> {
                        _destination.send(SplashDestination.Onboarding)
                        return@collect
                    }

                    // v3.x name — Supabase is still reading the saved session from disk.
                    // Stay on splash and wait for the next emission.
                    is SessionStatus.Initializing -> Unit

                    // v3.x name — token refresh failed after a network problem.
                    // A local session likely still exists; send to Home and let
                    // Supabase retry the refresh silently in the background.
                    is SessionStatus.RefreshFailure -> {
                        _destination.send(SplashDestination.Home)
                        return@collect
                    }
                }
            }
        }
    }
}