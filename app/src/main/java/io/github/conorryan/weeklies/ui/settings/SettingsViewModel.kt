package io.github.conorryan.weeklies.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.R
import io.github.conorryan.weeklies.data.backup.BackupErrors
import io.github.conorryan.weeklies.data.model.AppColorScheme
import io.github.conorryan.weeklies.data.model.AppSettings
import io.github.conorryan.weeklies.data.model.isValidPortionsPerDay
import io.github.conorryan.weeklies.ui.ToastBus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant

/** Configured off-device backup folder; null [uri] means no folder picked yet. */
data class BackupFolderState(
    val uri: String? = null,
    val name: String? = null,
    val autoBackup: Boolean = false,
    val lastBackupAt: Instant? = null,
) {
    val configured: Boolean get() = uri != null
}

data class SettingsUiState(
    val settings: AppSettings,
    val apiKey: String = "",
    val backupBusy: Boolean = false,
    val backupFolder: BackupFolderState = BackupFolderState(),
)

class SettingsViewModel(
    private val graph: AppGraph,
    private val toastBus: ToastBus,
) : ViewModel() {

    private val _busy = kotlinx.coroutines.flow.MutableStateFlow(false)

    private val backupFolderState = combine(
        graph.backupFolderStore.backupFolderUri,
        graph.backupFolderStore.backupFolderName,
        graph.backupFolderStore.autoBackupEnabled,
        graph.backupFolderStore.lastFolderBackupAt,
    ) { uri, name, autoBackup, lastBackupAt ->
        BackupFolderState(
            uri = uri,
            name = name,
            autoBackup = autoBackup,
            lastBackupAt = lastBackupAt,
        )
    }

    val state: StateFlow<SettingsUiState> = combine(
        graph.settingsStore.settings,
        graph.secretsStore.mistralApiKey,
        _busy,
        backupFolderState,
    ) { settings, apiKey, busy, folder ->
        SettingsUiState(
            settings = settings,
            apiKey = apiKey,
            backupBusy = busy,
            backupFolder = folder,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState(graph.settingsStore.defaults))

    fun setColorScheme(dark: Boolean) {
        viewModelScope.launch {
            graph.settingsStore.setColorScheme(if (dark) AppColorScheme.DARK else AppColorScheme.LIGHT)
        }
    }

    /** Persists [text] if it is a valid portions count; returns whether it was applied. */
    fun applyPortions(text: String): Boolean {
        val n = text.trim().toDoubleOrNull()
        if (n == null || !isValidPortionsPerDay(n)) return false
        viewModelScope.launch { graph.settingsStore.setPortionsPerDay(n.toInt()) }
        return true
    }

    fun saveApiKey(key: String) {
        viewModelScope.launch { graph.secretsStore.setMistralApiKey(key.trim()) }
    }

    fun clearApiKey() {
        viewModelScope.launch { graph.secretsStore.clearMistralApiKey() }
    }

    /** Writes a backup to [open] the user-picked destination; owns busy state and error copy. */
    fun exportBackup(open: () -> OutputStream) {
        runBackup(BackupAction.EXPORT) {
            open().use { out -> out.write(graph.backupManager.buildBackupJson().toByteArray(Charsets.UTF_8)) }
        }
    }

    /** Reads a backup from [read] the user-picked source and replaces all data; owns busy state and error copy. */
    fun importBackup(read: () -> InputStream) {
        runBackup(BackupAction.IMPORT) {
            val text = read().use { it.readBytes().toString(Charsets.UTF_8) }
            graph.backupManager.applyBackupImport(text)
        }
    }

    /** Persists the folder the user just picked, keeping its tree permission across reboots. */
    fun chooseBackupFolder(uri: Uri) {
        viewModelScope.launch {
            try {
                graph.folderBackups.takePersistablePermission(uri)
                val name = graph.folderBackups.folderDisplayName(uri) ?: ""
                graph.backupFolderStore.setBackupFolder(uri.toString(), name)
                toastBus.info(R.string.backup_folder_set)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                toastBus.error(
                    R.string.backup_folder_error_title,
                    BackupErrors.formatBackupError(e, BackupErrors.Kind.EXPORT),
                )
            }
        }
    }

    /** Forgets the backup folder and releases its tree permission. */
    fun clearBackupFolder() {
        viewModelScope.launch {
            state.value.backupFolder.uri?.let { graph.folderBackups.releasePersistablePermission(Uri.parse(it)) }
            graph.backupFolderStore.clearBackupFolder()
            toastBus.info(R.string.backup_folder_removed)
        }
    }

    fun setAutoBackup(enabled: Boolean) {
        viewModelScope.launch { graph.backupFolderStore.setAutoBackupEnabled(enabled) }
    }

    /** Writes a timestamped backup into the configured folder. */
    fun backupNowToFolder() {
        runBackup(BackupAction.BACKUP, successMessage = R.string.backup_done) {
            if (!graph.folderBackups.writeBackupNow()) {
                error("No backup folder configured")
            }
        }
    }

    /** One kind of backup run: the error category plus the copy for its canceled and failed toasts. */
    private enum class BackupAction(
        val errorKind: BackupErrors.Kind,
        val canceledMessage: Int,
        val failedTitle: Int,
    ) {
        EXPORT(BackupErrors.Kind.EXPORT, R.string.export_canceled_msg, R.string.export_failed_title),
        IMPORT(BackupErrors.Kind.IMPORT, R.string.import_canceled_msg, R.string.import_failed_title),
        BACKUP(BackupErrors.Kind.EXPORT, R.string.backup_canceled_msg, R.string.backup_failed_title),
    }

    private fun runBackup(
        action: BackupAction,
        successMessage: Int? = null,
        block: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            _busy.update { true }
            try {
                block()
                if (successMessage != null) toastBus.info(successMessage)
            } catch (e: kotlinx.coroutines.CancellationException) {
                toastBus.info(action.canceledMessage)
            } catch (e: Exception) {
                toastBus.error(
                    action.failedTitle,
                    BackupErrors.formatBackupError(e, action.errorKind),
                )
            } finally {
                _busy.update { false }
            }
        }
    }

    class Factory(private val graph: AppGraph, private val toastBus: ToastBus) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = SettingsViewModel(graph, toastBus) as T
    }
}
