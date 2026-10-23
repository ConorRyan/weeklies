package io.github.conorryan.weeklies

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import io.github.conorryan.weeklies.data.backup.AutoBackup
import io.github.conorryan.weeklies.data.backup.BackupManager
import io.github.conorryan.weeklies.data.backup.FolderBackups
import io.github.conorryan.weeklies.data.db.AppDatabase
import io.github.conorryan.weeklies.data.import.MistralClient
import io.github.conorryan.weeklies.data.import.OkHttpTransport
import io.github.conorryan.weeklies.data.import.PageFetcher
import io.github.conorryan.weeklies.data.import.RecipeCleaner
import io.github.conorryan.weeklies.data.import.RecipeImporter
import io.github.conorryan.weeklies.data.model.Recipe
import io.github.conorryan.weeklies.data.repo.PlanRepository
import io.github.conorryan.weeklies.data.repo.RecipeRepository
import io.github.conorryan.weeklies.data.repo.ShoppingRepository
import io.github.conorryan.weeklies.data.settings.SecretsStore
import io.github.conorryan.weeklies.data.settings.SettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant

/** Manual dependency graph; one instance per process, owned by the Application. */
class AppGraph(
    context: Context,
    db: AppDatabase,
    settingsStore: SettingsStore,
    secretsStore: SecretsStore,
) {
    val recipeRepository = RecipeRepository(db)
    val planRepository = PlanRepository(db)
    val shoppingRepository = ShoppingRepository(db)
    val settingsStore = settingsStore
    val secretsStore = secretsStore
    val backupManager = BackupManager(db, recipeRepository, planRepository, shoppingRepository, settingsStore)

    /** Writes backups to the SAF folder picked in Settings (off-device copy). */
    val folderBackups = FolderBackups(context, backupManager, settingsStore)

    /** Application-lived scope for background work like auto-backup. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Warm, app-lived state. Compose screens collect these with the StateFlow
     * overload of collectAsStateWithLifecycle, which seeds the first composed
     * frame from `.value` synchronously. That way a screen pushed onto the
     * NavHost renders real data in its first frame instead of flashing a
     * placeholder (NavHost is AnimatedContent-based, so a blank first
     * composition visibly animates in).
     */
    val recipesState: StateFlow<List<Recipe>> = recipeRepository.recipes
        .stateIn(appScope, SharingStarted.Lazily, emptyList())

    val mistralKeyState: StateFlow<String?> = secretsStore.mistralApiKey
        .stateIn(appScope, SharingStarted.Eagerly, null)

    val autoBackup = AutoBackup(
        enabled = combine(
            settingsStore.backupFolderUri,
            settingsStore.autoBackupEnabled,
        ) { uri, on -> uri != null && on },
        dataChanges = combine(
            recipeRepository.recipes,
            planRepository.byDay,
            shoppingRepository.items,
        ) { recipes, byDay, items -> Triple(recipes, byDay, items) }
            .distinctUntilChanged(),
        write = {
            val uri = settingsStore.backupFolderUri.first()
            if (uri != null) {
                folderBackups.writeBackup(Uri.parse(uri))
                settingsStore.recordFolderBackup(Instant.now())
            }
        },
        onError = { e -> Log.w("AutoBackup", "Automatic backup failed", e) },
    )

    val recipeImporter: RecipeImporter by lazy {
        val transport = OkHttpTransport()
        RecipeImporter(PageFetcher(transport), MistralClient(transport))
    }

    /** AI cleanup of a saved recipe's ingredient lines. */
    val recipeCleaner: RecipeCleaner by lazy { RecipeCleaner(OkHttpTransport()) }
}

class WeekliesApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.build(this)
        val settings = SettingsStore(this)
        graph = AppGraph(this, db, settings, SecretsStore(this))
        graph.autoBackup.start(graph.appScope)
    }
}
