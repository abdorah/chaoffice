package org.chaos.office.services

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class Settings(private val dataStore: DataStore<Preferences>) {
    private val LAST_SEEN_VERSION = stringPreferencesKey("last_seen_version")

    val lastSeenVersion: Flow<String> = dataStore.data
        .map { preferences ->
            preferences[LAST_SEEN_VERSION] ?: ""
        }

    suspend fun setLastSeenVersion(version: String) {
        dataStore.edit { preferences ->
            preferences[LAST_SEEN_VERSION] = version
        }
    }
}

expect fun createDataStore(producePath: () -> String): DataStore<Preferences>
