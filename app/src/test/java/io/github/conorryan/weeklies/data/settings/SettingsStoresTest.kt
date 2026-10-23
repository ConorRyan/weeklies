package io.github.conorryan.weeklies.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import io.github.conorryan.weeklies.data.model.AppColorScheme
import io.github.conorryan.weeklies.data.model.AppSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.time.Instant
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppSettingsStoreTest {

    private lateinit var context: Context
    private lateinit var store: AppSettingsStore
    private lateinit var dataStore: DataStore<Preferences>

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dataStore = newDataStore()
        store = AppSettingsStore(context, dataStore)
    }

    private fun newDataStore(): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { File(context.cacheDir, "test-${UUID.randomUUID()}.preferences_pb") },
    )

    @Test
    fun emitsStoredSettings() = runBlocking {
        store.setColorScheme(AppColorScheme.DARK)
        store.setPortionsPerDay(5)

        val settings = store.settings.first()
        assertEquals(AppColorScheme.DARK, settings.colorScheme)
        assertEquals(5, settings.portionsPerDay)
    }

    @Test
    fun ignoresInvalidPortions() = runBlocking {
        store.setPortionsPerDay(0)

        assertEquals(AppSettings.DEFAULT_PORTIONS_PER_DAY, store.settings.first().portionsPerDay)
    }

    @Test
    fun replaceWritesSchemeAndPortions() = runBlocking {
        store.replace(AppSettings(AppColorScheme.LIGHT, portionsPerDay = 4))

        assertEquals(AppColorScheme.LIGHT, store.current().colorScheme)
        assertEquals(4, store.current().portionsPerDay)
    }

    @Test
    fun replaceDropsInvalidPortionsToDefault() = runBlocking {
        store.setPortionsPerDay(3)
        store.replace(AppSettings(AppColorScheme.DARK, portionsPerDay = -1))

        assertEquals(AppSettings.DEFAULT_PORTIONS_PER_DAY, store.current().portionsPerDay)
    }

    @Test
    fun invalidStoredSchemeFallsBackToOsDefault() = runBlocking {
        dataStore.edit { it[stringPreferencesKey("colorScheme")] = "bogus" }

        val colorScheme = store.settings.first().colorScheme
        assertTrue(AppColorScheme.entries.contains(colorScheme))
        assertEquals(store.defaults.colorScheme, colorScheme)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupFolderStoreTest {

    private lateinit var context: Context
    private lateinit var store: BackupFolderStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        store = BackupFolderStore(
            PreferenceDataStoreFactory.create(
                produceFile = { File(context.cacheDir, "test-${UUID.randomUUID()}.preferences_pb") },
            ),
        )
    }

    @Test
    fun startsUnconfiguredWithAutoBackupOff() = runBlocking {
        assertNull(store.backupFolderUri.first())
        assertNull(store.backupFolderName.first())
        assertEquals(false, store.autoBackupEnabled.first())
        assertNull(store.lastFolderBackupAt.first())
    }

    @Test
    fun setBackupFolderClearsRecordedDocumentIds() = runBlocking {
        store.recordFolderBackupDocument("doc-1")
        store.setBackupFolder("content://tree/x", "Drive")

        assertEquals("content://tree/x", store.backupFolderUri.first())
        assertEquals("Drive", store.backupFolderName.first())
        assertEquals(emptySet<String>(), store.folderBackupDocumentIds.first())
    }

    @Test
    fun clearBackupFolderForgetsEverything() = runBlocking {
        store.setBackupFolder("content://tree/x", "Drive")
        store.recordFolderBackupDocument("doc-1")
        store.setAutoBackupEnabled(true)

        store.clearBackupFolder()

        assertNull(store.backupFolderUri.first())
        assertNull(store.backupFolderName.first())
        assertEquals(emptySet<String>(), store.folderBackupDocumentIds.first())
    }

    @Test
    fun recordsAndTrimsDocumentIds() = runBlocking {
        store.recordFolderBackupDocument("doc-1")
        store.recordFolderBackupDocument("doc-2")

        assertEquals(setOf("doc-1", "doc-2"), store.folderBackupDocumentIds.first())

        store.trimFolderBackupDocumentIds(listOf("doc-2"))
        assertEquals(setOf("doc-2"), store.folderBackupDocumentIds.first())
    }

    @Test
    fun recordsLastBackupTimestamp() = runBlocking {
        val at = Instant.parse("2026-10-23T12:00:00Z")
        store.recordFolderBackup(at)

        assertEquals(at, store.lastFolderBackupAt.first())
    }

    @Test
    fun unparseableTimestampEmitsNull() = runBlocking {
        val raw = PreferenceDataStoreFactory.create(
            produceFile = { File(context.cacheDir, "test-${UUID.randomUUID()}.preferences_pb") },
        )
        raw.edit { it[stringPreferencesKey("lastFolderBackupAt")] = "not-an-instant" }
        val store = BackupFolderStore(raw)

        assertNull(store.lastFolderBackupAt.first())
    }
}
