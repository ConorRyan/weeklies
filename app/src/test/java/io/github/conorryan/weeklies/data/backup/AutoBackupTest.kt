package io.github.conorryan.weeklies.data.backup

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AutoBackupTest {

    @Test
    fun launchingAloneNeverWritesABackup() = runTest {
        val enabled = MutableStateFlow(true)
        val data = MutableStateFlow(0)
        val writes = mutableListOf<Int>()

        AutoBackup(enabled, data, debounceMillis = 1_000, write = { writes.add(data.value) })
            .start(backgroundScope)

        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(0, writes.size)
    }

    @Test
    fun writesOnceAfterDataChangesWhileEnabled() = runTest {
        val enabled = MutableStateFlow(true)
        val data = MutableStateFlow(0)
        val writes = mutableListOf<Int>()

        AutoBackup(enabled, data, debounceMillis = 1_000, write = { writes.add(data.value) })
            .start(backgroundScope)

        advanceTimeBy(5_000)
        data.value = 1
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(listOf(1), writes)
    }

    @Test
    fun debounceCoalescesRapidChangesIntoOneWrite() = runTest {
        val enabled = MutableStateFlow(true)
        val data = MutableStateFlow(0)
        val writes = mutableListOf<Int>()

        AutoBackup(enabled, data, debounceMillis = 1_000, write = { writes.add(data.value) })
            .start(backgroundScope)

        advanceTimeBy(5_000)
        data.value = 1
        advanceTimeBy(500)
        data.value = 2
        advanceTimeBy(500)
        data.value = 3
        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(listOf(3), writes)
    }

    @Test
    fun dataChangesWhileDisabledDoNotWriteButEnablingDoes() = runTest {
        val enabled = MutableStateFlow(false)
        val data = MutableStateFlow(0)
        val writes = mutableListOf<Int>()

        AutoBackup(enabled, data, debounceMillis = 1_000, write = { writes.add(data.value) })
            .start(backgroundScope)

        advanceTimeBy(5_000)
        data.value = 1
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(0, writes.size)

        enabled.value = true
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(listOf(1), writes)
    }

    @Test
    fun writeFailureDoesNotStopLaterBackups() = runTest {
        val enabled = MutableStateFlow(true)
        val data = MutableStateFlow(0)
        val attempts = mutableListOf<Int>()

        AutoBackup(
            enabled,
            data,
            debounceMillis = 1_000,
            write = {
                attempts.add(data.value)
                error("folder unreachable")
            },
        ).start(backgroundScope)

        advanceTimeBy(5_000)
        data.value = 1
        advanceTimeBy(1_000)
        runCurrent()
        data.value = 2
        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(listOf(1, 2), attempts)
    }
}
