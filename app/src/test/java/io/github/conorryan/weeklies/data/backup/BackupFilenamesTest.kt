package io.github.conorryan.weeklies.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class BackupFilenamesTest {

    @Test
    fun timestampedFilenameMatchesExpectedShape() {
        val name = BackupFilenames.timestampedFilename(Instant.parse("2026-10-21T14:30:05Z"))
        assertTrue(name.matches(Regex("""weeklies-backup-\d{4}-\d{2}-\d{2}-\d{6}\.json""")))
        assertTrue(BackupFilenames.isFolderBackupFilename(name))
    }

    @Test
    fun timestampedNamesSortChronologicallyAcrossDays() {
        val zone = ZoneId.systemDefault()
        val earlier = BackupFilenames.timestampedFilename(
            Instant.parse("2026-10-20T23:59:00Z").atZone(zone).toInstant(),
        )
        val later = BackupFilenames.timestampedFilename(
            Instant.parse("2026-10-21T00:01:00Z").atZone(zone).toInstant(),
        )
        assertTrue(earlier < later)
    }

    @Test
    fun onlyExactTimestampedNamesCountAsFolderBackups() {
        assertTrue(BackupFilenames.isFolderBackupFilename("weeklies-backup-2026-10-21-143005.json"))
        // Names without the "weeklies-" prefix (another app's convention) never match.
        assertFalse(BackupFilenames.isFolderBackupFilename("backup-2026-10-21-143005.json"))
        assertFalse(BackupFilenames.isFolderBackupFilename("weeklies-2026-10-21-143005.json"))
        // Date-only names (single-file exports, other tools) are not prunable.
        assertFalse(BackupFilenames.isFolderBackupFilename("weeklies-backup-2026-10-21.json"))
        assertFalse(BackupFilenames.isFolderBackupFilename("weeklies-backup-2026-09-30.json"))
        // Foreign or malformed names are never prunable.
        assertFalse(BackupFilenames.isFolderBackupFilename("notes.txt"))
        assertFalse(BackupFilenames.isFolderBackupFilename("photo.jpg"))
        assertFalse(BackupFilenames.isFolderBackupFilename("backup.txt"))
        assertFalse(BackupFilenames.isFolderBackupFilename("2026-10-21.json"))
        assertFalse(BackupFilenames.isFolderBackupFilename("weeklies-backup-2026-10-21-1430.json"))
        assertFalse(BackupFilenames.isFolderBackupFilename("weeklies-backup-2026-10-21-143005.json.bak"))
    }

    @Test
    fun pruneTargetsDropOldestBeyondKeep() {
        val files = (1..6).map {
            FolderFile("doc-$it", "weeklies-backup-2026-10-${it.toString().padStart(2, '0')}-000000.json")
        }
        val targets = BackupFilenames.pruneTargets(files, keep = 3)

        assertEquals(
            listOf("doc-1", "doc-2", "doc-3"),
            targets.map { it.documentId },
        )
    }

    @Test
    fun pruneTargetsKeepEverythingWhenUnderLimit() {
        val files = listOf(
            FolderFile("doc-1", "weeklies-backup-2026-10-01-000000.json"),
            FolderFile("doc-2", "weeklies-backup-2026-10-02-000000.json"),
        )
        assertEquals(emptyList<FolderFile>(), BackupFilenames.pruneTargets(files, keep = 3))
    }

    @Test
    fun pruneTargetsNeverSelectFilesThatStoppedMatchingThePattern() {
        // Recorded document whose name was changed or does not match anymore:
        // kept regardless of the recorded ID.
        val files = listOf(
            FolderFile("renamed-1", "my-renamed-backup.json"),
            FolderFile("doc-1", "weeklies-backup-2026-10-01-000000.json"),
            FolderFile("doc-2", "weeklies-backup-2026-10-02-000000.json"),
            FolderFile("doc-3", "weeklies-backup-2026-10-03-000000.json"),
            FolderFile("doc-4", "weeklies-backup-2026-10-04-000000.json"),
        )
        val targets = BackupFilenames.pruneTargets(files, keep = 3)

        assertEquals(listOf("doc-1"), targets.map { it.documentId })
    }

    @Test
    fun sortedBackupsAreOldestFirst() {
        val names = listOf(
            "weeklies-backup-2026-10-21-143005.json",
            "weeklies-backup-2026-10-19-090000.json",
            "weeklies-backup-2026-10-20-000001.json",
        )
        assertEquals(
            listOf(
                "weeklies-backup-2026-10-19-090000.json",
                "weeklies-backup-2026-10-20-000001.json",
                "weeklies-backup-2026-10-21-143005.json",
            ),
            BackupFilenames.sortedBackups(names),
        )
    }
}
