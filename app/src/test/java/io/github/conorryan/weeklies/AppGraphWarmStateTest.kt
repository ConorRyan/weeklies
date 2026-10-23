package io.github.conorryan.weeklies

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.conorryan.weeklies.data.db.AppDatabase
import io.github.conorryan.weeklies.data.model.NewRecipe
import io.github.conorryan.weeklies.data.model.Recipe
import io.github.conorryan.weeklies.data.settings.AppSettingsStore
import io.github.conorryan.weeklies.data.settings.BackupFolderStore
import io.github.conorryan.weeklies.data.settings.SecretsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The app-lived warm StateFlows in [AppGraph] back first-frame-complete
 * screens: a pushed screen seeds its first composition from `.value`, so
 * these tests pin the two properties the UI relies on.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppGraphWarmStateTest {

    private fun buildGraph(): AppGraph {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.build(context)
        return AppGraph(
            context, db,
            AppSettingsStore.create(context),
            BackupFolderStore.create(context),
            SecretsStore.create(context),
        )
    }

    /** Waits until the predicate holds; the graph collects on Dispatchers.IO. */
    private suspend fun awaitTrue(timeoutMs: Long = 5_000, predicate: () -> Boolean) {
        withTimeout(timeoutMs) {
            while (!predicate()) delay(10)
        }
    }

    @Test
    fun recipesStateSeedsEmptyThenTracksTheDatabase() = runBlocking {
        val graph = buildGraph()

        // Seeding value: a screen's first frame reads this before Room emits.
        assertEquals(emptyList<Recipe>(), graph.recipesState.value)

        // Lazily: the upstream Room query only starts once someone collects.
        val collector = CoroutineScope(Dispatchers.IO + Job())
        val collect = collector.launch { graph.recipesState.collect { } }

        val added = graph.recipeRepository.addRecipe(NewRecipe("Pasta", listOf("200g pasta")))
        awaitTrue { graph.recipesState.value.any { it.id == added.id } }
        assertEquals("Pasta", graph.recipesState.value.single { it.id == added.id }.name)

        // Live updates keep flowing after the warm read.
        graph.recipeRepository.updateRecipe(added.id, NewRecipe("Pasta al pomodoro", listOf("200g pasta")))
        awaitTrue { graph.recipesState.value.any { it.name == "Pasta al pomodoro" } }

        collect.cancel()
    }

    @Test
    fun mistralKeyStateLoadsEagerlyAndTracksTheStoredKey() = runBlocking {
        val graph = buildGraph()

        // Eagerly: the stored key is loaded at startup without any collector,
        // so ImportScreen knows it in its first frame.
        awaitTrue { graph.mistralKeyState.value != null }

        graph.secretsStore.setMistralApiKey("k-123")
        awaitTrue { graph.mistralKeyState.value == "k-123" }
        assertEquals("k-123", graph.mistralKeyState.value)
    }
}
