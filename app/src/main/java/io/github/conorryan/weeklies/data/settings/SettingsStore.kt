package io.github.conorryan.weeklies.data.settings

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.conorryan.weeklies.data.model.AppColorScheme
import io.github.conorryan.weeklies.data.model.AppSettings
import io.github.conorryan.weeklies.data.model.isValidPortionsPerDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant

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

private val BACKUP_FOLDER_URI_KEY = stringPreferencesKey("backupFolderUri")
private val BACKUP_FOLDER_NAME_KEY = stringPreferencesKey("backupFolderName")
private val AUTO_BACKUP_ENABLED_KEY = booleanPreferencesKey("autoBackupEnabled")
private val LAST_FOLDER_BACKUP_KEY = stringPreferencesKey("lastFolderBackupAt")
private val FOLDER_BACKUP_DOCUMENT_IDS_KEY = stringSetPreferencesKey("folderBackupDocumentIds")

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

    /** First-frame value for collecting [settings]: defaults with the OS color-scheme fallback applied. */
    val defaults: AppSettings
        get() = AppSettings(
            colorScheme = defaultColorScheme(),
            portionsPerDay = AppSettings.DEFAULT_PORTIONS_PER_DAY,
        )

    /** Current settings snapshot, applying defaults for anything missing. */
    suspend fun current(): AppSettings = withDefaults(context.settingsDataStore.data.first())

    suspend fun setColorScheme(scheme: AppColorScheme) {
        context.settingsDataStore.edit { it[COLOR_SCHEME_KEY] = scheme.key }
    }

    suspend fun setPortionsPerDay(portions: Int) {
        if (!isValidPortionsPerDay(portions)) return
        context.settingsDataStore.edit { it[PORTIONS_PER_DAY_KEY] = portions }
    }

    /**
     * Configured SAF backup folder. Deliberately outside [AppSettings]: these
     * values are device-local, never travel inside backup JSON, and survive
     * backup imports (which only replace color scheme and portions).
     */
    val backupFolderUri: Flow<String?> =
        context.settingsDataStore.data.map { it[BACKUP_FOLDER_URI_KEY] }

    val backupFolderName: Flow<String?> =
        context.settingsDataStore.data.map { it[BACKUP_FOLDER_NAME_KEY] }

    val autoBackupEnabled: Flow<Boolean> =
        context.settingsDataStore.data.map { it[AUTO_BACKUP_ENABLED_KEY] ?: false }

    val lastFolderBackupAt: Flow<Instant?> = context.settingsDataStore.data.map { prefs ->
        prefs[LAST_FOLDER_BACKUP_KEY]?.let { runCatching { Instant.parse(it) }.getOrNull() }
    }

    suspend fun setBackupFolder(uri: String, name: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[BACKUP_FOLDER_URI_KEY] = uri
            prefs[BACKUP_FOLDER_NAME_KEY] = name
            prefs[FOLDER_BACKUP_DOCUMENT_IDS_KEY] = emptySet()
        }
    }

    suspend fun clearBackupFolder() {
        context.settingsDataStore.edit { prefs ->
            prefs.remove(BACKUP_FOLDER_URI_KEY)
            prefs.remove(BACKUP_FOLDER_NAME_KEY)
            prefs.remove(FOLDER_BACKUP_DOCUMENT_IDS_KEY)
        }
    }

    /**
     * SAF document IDs of backups this app created in the current folder.
     * Pruning may delete only these; anything else in the folder is safe even
     * if it happens to be named like a backup.
     */
    val folderBackupDocumentIds: Flow<Set<String>> =
        context.settingsDataStore.data.map { it[FOLDER_BACKUP_DOCUMENT_IDS_KEY] ?: emptySet() }

    suspend fun recordFolderBackupDocument(documentId: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[FOLDER_BACKUP_DOCUMENT_IDS_KEY] =
                (prefs[FOLDER_BACKUP_DOCUMENT_IDS_KEY] ?: emptySet()) + documentId
        }
    }

    /** Keeps only [keepIds] as the recorded set. */
    suspend fun trimFolderBackupDocumentIds(keepIds: Collection<String>) {
        context.settingsDataStore.edit { prefs ->
            prefs[FOLDER_BACKUP_DOCUMENT_IDS_KEY] = keepIds.toSet()
        }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[AUTO_BACKUP_ENABLED_KEY] = enabled }
    }

    suspend fun recordFolderBackup(at: Instant) {
        context.settingsDataStore.edit { it[LAST_FOLDER_BACKUP_KEY] = at.toString() }
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
