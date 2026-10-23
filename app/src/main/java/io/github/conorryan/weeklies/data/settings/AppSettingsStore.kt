package io.github.conorryan.weeklies.data.settings

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.conorryan.weeklies.data.model.AppColorScheme
import io.github.conorryan.weeklies.data.model.AppSettings
import io.github.conorryan.weeklies.data.model.isValidPortionsPerDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val COLOR_SCHEME_KEY = stringPreferencesKey("colorScheme")
private val PORTIONS_PER_DAY_KEY = intPreferencesKey("portionsPerDay")
private val SETTINGS_SCHEMA_VERSION_KEY = intPreferencesKey("settingsSchemaVersion")

/**
 * App settings (color scheme, portions per day). The stored shape mirrors the
 * previous zustand persistence: a record with an [AppColorScheme] key,
 * portions per day, and a schema version for future migrations.
 *
 * Device-local backup-folder settings deliberately live elsewhere
 * ([BackupFolderStore]): they never travel inside backup JSON and survive
 * backup imports, which only replace color scheme and portions.
 */
class AppSettingsStore(
    private val context: Context,
    private val dataStore: DataStore<Preferences>,
) {

    val settings: Flow<AppSettings> = dataStore.data.map { prefs ->
        val scheme = AppColorScheme.fromKey(prefs[COLOR_SCHEME_KEY])
            ?: defaultColorScheme()
        val portions = prefs[PORTIONS_PER_DAY_KEY]
        AppSettings(
            colorScheme = scheme,
            portionsPerDay = if (portions != null && isValidPortionsPerDay(portions)) portions
            else AppSettings.DEFAULT_PORTIONS_PER_DAY,
        )
    }

    /** First-frame value for collecting [settings]: defaults with the OS color-scheme fallback applied. */
    val defaults: AppSettings
        get() = AppSettings(
            colorScheme = defaultColorScheme(),
            portionsPerDay = AppSettings.DEFAULT_PORTIONS_PER_DAY,
        )

    /** Current settings snapshot, applying defaults for anything missing. */
    suspend fun current(): AppSettings = withDefaults(dataStore.data.first())

    suspend fun setColorScheme(scheme: AppColorScheme) {
        dataStore.edit { it[COLOR_SCHEME_KEY] = scheme.key }
    }

    suspend fun setPortionsPerDay(portions: Int) {
        if (!isValidPortionsPerDay(portions)) return
        dataStore.edit { it[PORTIONS_PER_DAY_KEY] = portions }
    }

    /** Replaces everything; invalid values fall back to defaults. */
    suspend fun replace(next: AppSettings) {
        dataStore.edit { prefs ->
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

    /**
     * Mirrors the RN initialFromOs(): light only when the OS reports light,
     * dark otherwise (including unknown).
     */
    private fun defaultColorScheme(): AppColorScheme =
        if (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            == Configuration.UI_MODE_NIGHT_NO
        ) {
            AppColorScheme.LIGHT
        } else {
            AppColorScheme.DARK
        }

    companion object {
        const val SCHEMA_VERSION = 1

        fun create(context: Context): AppSettingsStore =
            AppSettingsStore(context, SettingsDataStores.settings(context))
    }
}
