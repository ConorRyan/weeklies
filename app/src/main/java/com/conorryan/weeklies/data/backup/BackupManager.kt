package com.conorryan.weeklies.data.backup

import androidx.room.withTransaction
import com.conorryan.weeklies.data.db.AppDatabase
import com.conorryan.weeklies.data.db.PlanEntryEntity
import com.conorryan.weeklies.data.db.RecipeEntity
import com.conorryan.weeklies.data.db.ShoppingItemEntity
import com.conorryan.weeklies.data.model.Recipe
import com.conorryan.weeklies.data.model.ShoppingItem
import com.conorryan.weeklies.data.model.Weekday
import com.conorryan.weeklies.data.model.emptyByDay
import com.conorryan.weeklies.data.settings.SettingsStore
import java.time.Instant

/**
 * Builds backups from the database and applies imported backups, replacing
 * everything currently stored (no merge), mirroring the RN app's behavior.
 */
class BackupManager(
    private val db: AppDatabase,
    private val settingsStore: SettingsStore,
) {

    fun defaultBackupFilename(): String = BackupCodec.defaultBackupFilename()

    suspend fun buildBackupJson(): String {
        val snapshot = BackupSnapshot(
            byDay = snapshotByDay(),
            recipes = db.recipeDao().getAll().map { it.toModel() },
            items = db.shoppingItemDao().getAll().map { it.toModel() },
        )
        val settings = settingsStore.current()
        return BackupCodec.buildBackupJson(settings, snapshot, Instant.now().toString())
    }

    /** Parses a backup file and replaces all app data with its contents. */
    suspend fun applyBackupImport(rawJson: String) {
        val defaults = settingsStore.current()
        val imported = BackupCodec.parseBackup(rawJson, defaults)

        db.withTransaction {
            db.recipeDao().replaceAll(imported.recipes.map { it.toEntity() })
            db.shoppingItemDao().replaceAll(
                imported.items.mapIndexed { index, item -> item.toEntity(index.toLong()) },
            )
            db.planDao().replaceAll(
                imported.byDay.map { (day, recipeId) ->
                    PlanEntryEntity(weekday = day.key, recipeId = recipeId)
                },
            )
        }
        settingsStore.replace(imported.settings)
    }

    private suspend fun snapshotByDay(): Map<Weekday, String?> {
        val next = emptyByDay().toMutableMap()
        for (row in db.planDao().getAll()) {
            val day = Weekday.fromKey(row.weekday) ?: continue
            next[day] = row.recipeId
        }
        return next.toMap()
    }

    private fun RecipeEntity.toModel() = Recipe(id = id, name = name, ingredients = ingredients)

    private fun ShoppingItemEntity.toModel() = ShoppingItem(id = id, label = label, checked = checked)

    private fun Recipe.toEntity() = RecipeEntity(id = id, name = name, ingredients = ingredients)

    private fun ShoppingItem.toEntity(position: Long) = ShoppingItemEntity(
        id = id,
        label = label,
        checked = checked,
        position = position,
    )
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
