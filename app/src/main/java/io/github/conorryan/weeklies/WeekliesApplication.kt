package io.github.conorryan.weeklies

import android.app.Application
import io.github.conorryan.weeklies.data.backup.BackupManager
import io.github.conorryan.weeklies.data.db.AppDatabase
import io.github.conorryan.weeklies.data.import.MistralClient
import io.github.conorryan.weeklies.data.import.OkHttpTransport
import io.github.conorryan.weeklies.data.import.PageFetcher
import io.github.conorryan.weeklies.data.import.RecipeImporter
import io.github.conorryan.weeklies.data.repo.PlanRepository
import io.github.conorryan.weeklies.data.repo.RecipeRepository
import io.github.conorryan.weeklies.data.repo.ShoppingRepository
import io.github.conorryan.weeklies.data.settings.SecretsStore
import io.github.conorryan.weeklies.data.settings.SettingsStore

/** Manual dependency graph; one instance per process, owned by the Application. */
class AppGraph(db: AppDatabase, settingsStore: SettingsStore, secretsStore: SecretsStore) {
    val recipeRepository = RecipeRepository(db)
    val planRepository = PlanRepository(db)
    val shoppingRepository = ShoppingRepository(db)
    val settingsStore = settingsStore
    val secretsStore = secretsStore
    val backupManager = BackupManager(db, recipeRepository, planRepository, shoppingRepository, settingsStore)

    val recipeImporter: RecipeImporter by lazy {
        val transport = OkHttpTransport()
        RecipeImporter(PageFetcher(transport), MistralClient(transport))
    }
}

class WeekliesApplication : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.build(this)
        val settings = SettingsStore(this)
        graph = AppGraph(db, settings, SecretsStore(this))
    }
}
