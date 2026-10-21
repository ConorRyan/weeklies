package io.github.conorryan.weeklies.data.backup

import androidx.room.withTransaction
import io.github.conorryan.weeklies.data.db.AppDatabase
import io.github.conorryan.weeklies.data.settings.SettingsStore
import io.github.conorryan.weeklies.data.repo.PlanRepository
import io.github.conorryan.weeklies.data.repo.RecipeRepository
import io.github.conorryan.weeklies.data.repo.ShoppingRepository
import java.time.Instant

/**
 * Builds backups from the repositories and applies imported backups, replacing
 * everything currently stored (no merge), mirroring the RN app's behavior.
 *
 * All reads and writes go through the repositories; the database handle is kept
 * only to scope the restore transaction across all three replacements.
 */
class BackupManager(
    private val db: AppDatabase,
    private val recipeRepository: RecipeRepository,
    private val planRepository: PlanRepository,
    private val shoppingRepository: ShoppingRepository,
    private val settingsStore: SettingsStore,
) {

    fun defaultBackupFilename(): String = BackupCodec.defaultBackupFilename()

    suspend fun buildBackupJson(): String {
        val snapshot = BackupSnapshot(
            byDay = planRepository.snapshot(),
            recipes = recipeRepository.snapshot(),
            items = shoppingRepository.snapshot(),
        )
        val settings = settingsStore.current()
        return BackupCodec.buildBackupJson(settings, snapshot, Instant.now().toString())
    }

    /** Parses a backup file and replaces all app data with its contents. */
    suspend fun applyBackupImport(rawJson: String) {
        val defaults = settingsStore.current()
        val imported = BackupCodec.parseBackup(rawJson, defaults)

        db.withTransaction {
            recipeRepository.replaceAll(imported.recipes)
            shoppingRepository.replaceAll(imported.items)
            planRepository.replaceAll(imported.byDay)
        }
        settingsStore.replace(imported.settings)
    }
}

/** Short, user-facing copy for backup failure toasts (no stack traces). */
object BackupErrors {

    fun isUserCanceled(error: Throwable?): Boolean {
        if (error is kotlinx.coroutines.CancellationException) return true
        val lower = (error?.message ?: "").lowercase()
        return lower.contains("cancel") ||
            lower.contains("dismissed") ||
            lower.contains("user dismissed")
    }

    fun formatBackupError(error: Throwable?, kind: Kind): String {
        if (isUserCanceled(error)) {
            return if (kind == Kind.EXPORT) "Export was canceled." else "Import was canceled."
        }

        val lower = (error?.message ?: "").lowercase()

        val permission = lower.contains("permission") ||
            lower.contains("eacces") ||
            lower.contains("access denied")
        if (permission) {
            return if (kind == Kind.EXPORT) {
                "Could not write there. Try another folder or check permissions."
            } else {
                "Could not read that file. Check permissions and try again."
            }
        }

        if (kind == Kind.IMPORT) {
            if (lower.contains("parse") ||
                lower.contains("invalid file") ||
                lower.contains("invalid backup") ||
                lower.contains("could not parse json")
            ) {
                return "That file is not a valid Weeklies backup."
            }
            if (lower.contains("unsupported backup")) {
                return "That backup version is not supported."
            }
            return "Could not import. Pick a backup JSON file from this app."
        }

        return "Could not save your backup. Try again or choose another folder."
    }

    enum class Kind { EXPORT, IMPORT }
}
