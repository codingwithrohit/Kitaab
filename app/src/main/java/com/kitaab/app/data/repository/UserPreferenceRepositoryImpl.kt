package com.kitaab.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
    }

    override val isProfileComplete: Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[Keys.PROFILE_COMPLETE] ?: false
        }

    override suspend fun setProfileComplete(complete: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PROFILE_COMPLETE] = complete
        }
    }
}