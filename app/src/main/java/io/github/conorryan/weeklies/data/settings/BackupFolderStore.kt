package io.github.conorryan.weeklies.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

private val BACKUP_FOLDER_URI_KEY = stringPreferencesKey("backupFolderUri")
private val BACKUP_FOLDER_NAME_KEY = stringPreferencesKey("backupFolderName")
private val AUTO_BACKUP_ENABLED_KEY = booleanPreferencesKey("autoBackupEnabled")
private val LAST_FOLDER_BACKUP_KEY = stringPreferencesKey("lastFolderBackupAt")
private val FOLDER_BACKUP_DOCUMENT_IDS_KEY = stringSetPreferencesKey("folderBackupDocumentIds")

/**
 * Device-local settings for the off-device backup folder. Deliberately
 * outside [io.github.conorryan.weeklies.data.model.AppSettings]: these values
 * never travel inside backup JSON and survive backup imports.
 *
 * This is the bookkeeping half of the folder-backup concept;
 * [io.github.conorryan.weeklies.data.backup.FolderBackups] is the half that
 * writes and prunes.
 */
class BackupFolderStore(private val dataStore: DataStore<Preferences>) {

    /** Configured SAF backup folder; null means no folder picked yet. */
    val backupFolderUri: Flow<String?> =
        dataStore.data.map { it[BACKUP_FOLDER_URI_KEY] }

    val backupFolderName: Flow<String?> =
        dataStore.data.map { it[BACKUP_FOLDER_NAME_KEY] }

    val autoBackupEnabled: Flow<Boolean> =
        dataStore.data.map { it[AUTO_BACKUP_ENABLED_KEY] ?: false }

    val lastFolderBackupAt: Flow<Instant?> = dataStore.data.map { prefs ->
        prefs[LAST_FOLDER_BACKUP_KEY]?.let { runCatching { Instant.parse(it) }.getOrNull() }
    }

    suspend fun setBackupFolder(uri: String, name: String) {
        dataStore.edit { prefs ->
            prefs[BACKUP_FOLDER_URI_KEY] = uri
            prefs[BACKUP_FOLDER_NAME_KEY] = name
            prefs[FOLDER_BACKUP_DOCUMENT_IDS_KEY] = emptySet()
        }
    }

    suspend fun clearBackupFolder() {
        dataStore.edit { prefs ->
            prefs.remove(BACKUP_FOLDER_URI_KEY)
            prefs.remove(BACKUP_FOLDER_NAME_KEY)
            prefs.remove(FOLDER_BACKUP_DOCUMENT_IDS_KEY)
        }
    }

    suspend fun setAutoBackupEnabled(enabled: Boolean) {
        dataStore.edit { it[AUTO_BACKUP_ENABLED_KEY] = enabled }
    }

    suspend fun recordFolderBackup(at: Instant) {
        dataStore.edit { it[LAST_FOLDER_BACKUP_KEY] = at.toString() }
    }

    /**
     * SAF document IDs of backups this app created in the current folder.
     * Pruning may delete only these; anything else in the folder is safe even
     * if it happens to be named like a backup.
     */
    val folderBackupDocumentIds: Flow<Set<String>> =
        dataStore.data.map { it[FOLDER_BACKUP_DOCUMENT_IDS_KEY] ?: emptySet() }

    suspend fun recordFolderBackupDocument(documentId: String) {
        dataStore.edit { prefs ->
            prefs[FOLDER_BACKUP_DOCUMENT_IDS_KEY] =
                (prefs[FOLDER_BACKUP_DOCUMENT_IDS_KEY] ?: emptySet()) + documentId
        }
    }

    /** Keeps only [keepIds] as the recorded set. */
    suspend fun trimFolderBackupDocumentIds(keepIds: Collection<String>) {
        dataStore.edit { prefs ->
            prefs[FOLDER_BACKUP_DOCUMENT_IDS_KEY] = keepIds.toSet()
        }
    }

    companion object {
        fun create(context: android.content.Context): BackupFolderStore =
            BackupFolderStore(SettingsDataStores.settings(context))
    }
}
