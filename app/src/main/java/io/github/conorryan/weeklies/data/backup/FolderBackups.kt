package io.github.conorryan.weeklies.data.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import io.github.conorryan.weeklies.data.settings.SettingsStore
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * A file seen in the backup folder, identified by its document ID and display
 * name. Document IDs come from the provider and are stable for the lifetime
 * of the file.
 */
data class FolderFile(val documentId: String, val name: String)

/**
 * Filenames for backups written to a configured folder, e.g.
 * "weeklies-backup-2026-10-21-143005.json". Fixed-width timestamps keep the
 * names sorting chronologically by plain string order, and the "weeklies-"
 * prefix makes the file's origin obvious at a glance.
 */
object BackupFilenames {

    const val PREFIX = "weeklies-backup-"
    const val SUFFIX = ".json"
    const val KEEP = 3

    private val timestampFormat =
        DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmmss").withZone(ZoneId.systemDefault())

    private val timestampedPattern = Regex("""weeklies-backup-\d{4}-\d{2}-\d{2}-\d{6}\.json""")

    fun timestampedFilename(at: Instant = Instant.now()): String =
        "$PREFIX${timestampFormat.format(at)}$SUFFIX"

    /**
     * True only for this app's timestamped folder backups. Looser names such
     * as the date-only export file ("weeklies-backup-2026-10-21.json") or
     * another tool's "backup-something.json" are deliberately not matched.
     */
    fun isFolderBackupFilename(name: String): Boolean = timestampedPattern.matches(name)

    /** This app's timestamped backup names from [names], oldest first. */
    fun sortedBackups(names: Collection<String>): List<String> =
        names.filter(::isFolderBackupFilename).sorted()

    /**
     * From files this app created (callers pass only those), the ones to
     * delete so the newest [keep] remain. The name-pattern check is a second
     * lock: a recorded document whose name somehow stopped matching is kept.
     */
    fun pruneTargets(files: List<FolderFile>, keep: Int = KEEP): List<FolderFile> =
        sortedBackups(files.map { it.name })
            .dropLast(keep)
            .toSet()
            .let { names -> files.filter { it.name in names } }
}

/**
 * Writes backups into a user-chosen SAF folder (a cloud provider such as
 * Google Drive or Nextcloud, or a USB drive). The folder is picked once via
 * OpenDocumentTree; the app holds a persisted tree permission and the provider
 * handles the off-device sync. No credentials are stored.
 *
 * Pruning is restricted to files this app itself created: every written
 * backup's document ID is recorded in [SettingsStore], and only recorded IDs
 * are ever deleted. Files placed in the folder by anything else are never
 * touched.
 */
class FolderBackups(
    private val context: Context,
    private val backupManager: BackupManager,
    private val settingsStore: SettingsStore,
) {
    private val resolver get() = context.contentResolver

    /**
     * Writes a timestamped backup into [treeUri] and prunes old backups
     * beyond [BackupFilenames.KEEP]. Returns the filename written. A
     * partially written file is deleted again on failure.
     */
    suspend fun writeBackup(treeUri: Uri): String {
        val filename = BackupFilenames.timestampedFilename()
        val document = DocumentsContract.createDocument(
            resolver,
            rootDocumentUri(treeUri),
            "application/json",
            filename,
        ) ?: error("Could not create a file in that folder")
        try {
            val output = resolver.openOutputStream(document)
                ?: error("Could not open the backup file for writing")
            output.use { it.write(backupManager.buildBackupJson().toByteArray(Charsets.UTF_8)) }
        } catch (e: Exception) {
            runCatching { DocumentsContract.deleteDocument(resolver, document) }
            throw e
        }
        val documentId = DocumentsContract.getDocumentId(document)
        settingsStore.recordFolderBackupDocument(documentId)
        pruneOldBackups(treeUri)
        return filename
    }

    /** Display name of the picked folder, or null if it cannot be queried. */
    fun folderDisplayName(treeUri: Uri): String? {
        val uri = rootDocumentUri(treeUri)
        resolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) return cursor.getString(0) }
        return null
    }

    /** Keeps the persisted read/write grant for the picked folder across reboots. */
    fun takePersistablePermission(treeUri: Uri) {
        resolver.takePersistableUriPermission(treeUri, readWriteFlags)
    }

    fun releasePersistablePermission(treeUri: Uri) {
        runCatching { resolver.releasePersistableUriPermission(treeUri, readWriteFlags) }
    }

    /**
     * Deletes this app's oldest backups beyond [BackupFilenames.KEEP].
     * Candidates must be both recorded as created by this app and still match
     * the timestamped name pattern. Also forgets recorded IDs of files that
     * no longer exist in the folder.
     */
    private suspend fun pruneOldBackups(treeUri: Uri) {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        val present = mutableListOf<FolderFile>()
        resolver.query(
            children,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                present.add(FolderFile(cursor.getString(0), cursor.getString(1)))
            }
        }
        val recorded = settingsStore.folderBackupDocumentIds.first()
        val own = present.filter { it.documentId in recorded }
        val targets = BackupFilenames.pruneTargets(own)
        for (file in targets) {
            val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, file.documentId)
            runCatching { DocumentsContract.deleteDocument(resolver, documentUri) }
        }
        val deletedIds = targets.map { it.documentId }.toSet()
        settingsStore.trimFolderBackupDocumentIds(
            own.map { it.documentId }.filter { it !in deletedIds },
        )
    }

    private fun rootDocumentUri(treeUri: Uri): Uri = DocumentsContract.buildDocumentUriUsingTree(
        treeUri,
        DocumentsContract.getTreeDocumentId(treeUri),
    )

    private companion object {
        const val readWriteFlags =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }
}
