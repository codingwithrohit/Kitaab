package com.kitaab.app.domain.repository

import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val isProfileComplete: Flow<Boolean>
    val city: Flow<String>
    val pincode: Flow<String>
    val locality: Flow<String>

    suspend fun setProfileComplete(complete: Boolean)
    suspend fun setLocation(city: String, pincode: String, locality: String = "")
}