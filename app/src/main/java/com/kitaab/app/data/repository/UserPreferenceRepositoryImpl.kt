package com.kitaab.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kitaab.app.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "kitaab_user_prefs",
)

@Singleton
class UserPreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : UserPreferencesRepository {

    private object Keys {
        val PROFILE_COMPLETE = booleanPreferencesKey("profile_complete")
        val CITY = stringPreferencesKey("city")
        val PINCODE = stringPreferencesKey("pincode")
        val LOCALITY = stringPreferencesKey("locality")
    }

    override val isProfileComplete: Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[Keys.PROFILE_COMPLETE] ?: false }

    override val city: Flow<String> =
        context.dataStore.data.map { prefs -> prefs[Keys.CITY] ?: "" }

    override val pincode: Flow<String> =
        context.dataStore.data.map { prefs -> prefs[Keys.PINCODE] ?: "" }

    override val locality: Flow<String> =
        context.dataStore.data.map { prefs -> prefs[Keys.LOCALITY] ?: "" }

    override suspend fun setProfileComplete(complete: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.PROFILE_COMPLETE] = complete }
    }

    override suspend fun setLocation(city: String, pincode: String, locality: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CITY] = city
            prefs[Keys.PINCODE] = pincode
            prefs[Keys.LOCALITY] = locality
        }
    }
}