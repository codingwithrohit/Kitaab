package com.kitaab.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {

    val isProfileComplete: Flow<Boolean>

    suspend fun setProfileComplete(complete: Boolean)
}