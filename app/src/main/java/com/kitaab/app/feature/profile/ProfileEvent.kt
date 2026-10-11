package com.kitaab.app.feature.profile

sealed interface ProfileEvent {
    data object SignedOut : ProfileEvent

    data object AccountDeleted : ProfileEvent
}
