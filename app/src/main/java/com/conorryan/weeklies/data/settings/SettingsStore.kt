package com.conorryan.weeklies.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.conorryan.weeklies.data.model.AppColorScheme
import com.conorryan.weeklies.data.model.AppSettings
import com.conorryan.weeklies.data.model.isValidPortionsPerDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "weeklies.settings",
)

private val Context.secretsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "weeklies.secrets",
)

private val COLOR_SCHEME_KEY = stringPreferencesKey("colorScheme")
private val PORTIONS_PER_DAY_KEY = intPreferencesKey("portionsPerDay")
private val SETTINGS_SCHEMA_VERSION_KEY = intPreferencesKey("settingsSchemaVersion")

private val MISTRAL_API_KEY = stringPreferencesKey("mistralApiKey")

/**
 * App settings (color scheme, portions per day), persisted with DataStore.
 *
 * The stored shape mirrors the previous zustand persistence: a record with
 * [AppColorScheme] key, portions per day, and a schema version for future
 * migrations.
 */
class SettingsStore(private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val scheme = AppColorScheme.fromKey(prefs[COLOR_SCHEME_KEY])
            ?: defaultColorScheme()
        val portions = prefs[PORTIONS_PER_DAY_KEY]
        AppSettings(
            colorScheme = scheme,
            portionsPerDay = if (portions != null && isValidPortionsPerDay(portions)) portions
            else AppSettings.DEFAULT_PORTIONS_PER_DAY,
        )
    }

    /** Current settings snapshot, applying defaults for anything missing. */
    suspend fun current(): AppSettings = withDefaults(context.settingsDataStore.data.first())

    suspend fun setColorScheme(scheme: AppColorScheme) {
        context.settingsDataStore.edit { it[COLOR_SCHEME_KEY] = scheme.key }
    }

    suspend fun setPortionsPerDay(portions: Int) {
        if (!isValidPortionsPerDay(portions)) return
        context.settingsDataStore.edit { it[PORTIONS_PER_DAY_KEY] = portions }
    }

    /** Replaces everything; invalid values fall back to defaults. */
    suspend fun replace(next: AppSettings) {
        context.settingsDataStore.edit { prefs ->
            prefs[COLOR_SCHEME_KEY] = next.colorScheme.key
            prefs[SETTINGS_SCHEMA_VERSION_KEY] = SCHEMA_VERSION
            if (isValidPortionsPerDay(next.portionsPerDay)) {
                prefs[PORTIONS_PER_DAY_KEY] = next.portionsPerDay
            } else {
                prefs.remove(PORTIONS_PER_DAY_KEY)
            }
        }
    }

    private fun withDefaults(prefs: Preferences): AppSettings = AppSettings(
        colorScheme = AppColorScheme.fromKey(prefs[COLOR_SCHEME_KEY]) ?: defaultColorScheme(),
        portionsPerDay = prefs[PORTIONS_PER_DAY_KEY]?.takeIf { isValidPortionsPerDay(it) }
            ?: AppSettings.DEFAULT_PORTIONS_PER_DAY,
    )

    companion object {
        const val SCHEMA_VERSION = 1

        /** Dark mode is the safe default before anything has been stored. */
        fun defaultColorScheme(): AppColorScheme = AppColorScheme.DARK
    }
}

/** Secrets (Mistral API key). Kept in a separate file and never part of backups. */
class SecretsStore(private val context: Context) {

    val mistralApiKey: Flow<String> = context.secretsDataStore.data.map { prefs ->
        prefs[MISTRAL_API_KEY] ?: ""
    }

    suspend fun setMistralApiKey(key: String) {
        context.secretsDataStore.edit { it[MISTRAL_API_KEY] = key }
    }

    suspend fun clearMistralApiKey() {
        context.secretsDataStore.edit { it.remove(MISTRAL_API_KEY) }
    }
}
