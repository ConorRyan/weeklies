package io.github.conorryan.weeklies.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val MISTRAL_API_KEY = stringPreferencesKey("mistralApiKey")

/** Secrets (Mistral API key). Kept in its own DataStore and never part of backups. */
class SecretsStore(private val dataStore: DataStore<Preferences>) {

    val mistralApiKey: Flow<String> = dataStore.data.map { prefs ->
        prefs[MISTRAL_API_KEY] ?: ""
    }

    suspend fun setMistralApiKey(key: String) {
        dataStore.edit { it[MISTRAL_API_KEY] = key }
    }

    suspend fun clearMistralApiKey() {
        dataStore.edit { it.remove(MISTRAL_API_KEY) }
    }

    companion object {
        fun create(context: android.content.Context): SecretsStore =
            SecretsStore(SettingsDataStores.secrets(context))
    }
}
