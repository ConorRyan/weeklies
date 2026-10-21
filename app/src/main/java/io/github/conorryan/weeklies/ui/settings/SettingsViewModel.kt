package io.github.conorryan.weeklies.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.conorryan.weeklies.AppGraph
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

data class SettingsUiState(
    val settings: AppSettings,
    val apiKey: String = "",
    val backupBusy: Boolean = false,
)

class SettingsViewModel(
    private val graph: AppGraph,
    private val toastBus: ToastBus,
) : ViewModel() {

    private val _busy = kotlinx.coroutines.flow.MutableStateFlow(false)
    val state: StateFlow<SettingsUiState> = combine(
        graph.settingsStore.settings,
        graph.secretsStore.mistralApiKey,
        _busy,
    ) { settings, apiKey, busy ->
        SettingsUiState(settings = settings, apiKey = apiKey, backupBusy = busy)
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
        runBackup(BackupErrors.Kind.EXPORT, "Export") {
            open().use { out -> out.write(graph.backupManager.buildBackupJson().toByteArray(Charsets.UTF_8)) }
        }
    }

    /** Reads a backup from [read] the user-picked source and replaces all data; owns busy state and error copy. */
    fun importBackup(read: () -> InputStream) {
        runBackup(BackupErrors.Kind.IMPORT, "Import") {
            val text = read().use { it.readBytes().toString(Charsets.UTF_8) }
            graph.backupManager.applyBackupImport(text)
        }
    }

    private fun runBackup(kind: BackupErrors.Kind, label: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _busy.update { true }
            try {
                block()
            } catch (e: kotlinx.coroutines.CancellationException) {
                toastBus.info("$label canceled")
            } catch (e: Exception) {
                toastBus.error(
                    "$label failed",
                    BackupErrors.formatBackupError(e, kind),
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
