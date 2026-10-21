package io.github.conorryan.weeklies.data.backup

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

/**
 * Writes a backup shortly after app data changes while auto-backup is on.
 *
 * The first emission after [start] only primes the baseline, so launching the
 * app alone never writes a backup; toggling auto-backup on counts as a change
 * and produces one write.
 */
class AutoBackup(
    private val enabled: Flow<Boolean>,
    private val dataChanges: Flow<Any?>,
    private val debounceMillis: Long = 30_000,
    private val write: suspend () -> Unit,
    private val onError: (Exception) -> Unit = {},
) {

    fun start(scope: CoroutineScope): Job = scope.launch {
        var primed = false
        combine(enabled, dataChanges) { on, _ -> on }
            .debounce(debounceMillis)
            .collect { on ->
                when {
                    !primed -> primed = true
                    on -> try {
                        write()
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // Keep observing; the last-backup timestamp stays stale
                        // so the settings screen reflects the missed backup.
                        onError(e)
                    }
                }
            }
    }
}
