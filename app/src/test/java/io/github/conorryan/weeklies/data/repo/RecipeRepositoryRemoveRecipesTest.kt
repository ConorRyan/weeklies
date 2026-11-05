package io.github.conorryan.weeklies.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.conorryan.weeklies.data.db.AppDatabase
import io.github.conorryan.weeklies.data.model.NewRecipe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Multiselect delete through the repository's interface: removes exactly the
 * requested recipes in one call, and ignores ids that no longer exist.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecipeRepositoryRemoveRecipesTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repo: RecipeRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = RecipeRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun removesExactlyTheGivenRecipes() = runBlocking {
        val a = repo.addRecipe(NewRecipe(name = "Pasta", ingredients = listOf("200 g pasta")))
        val b = repo.addRecipe(NewRecipe(name = "Soup", ingredients = listOf("1 onion")))
        val c = repo.addRecipe(NewRecipe(name = "Salad", ingredients = listOf("1 tomato")))

        repo.removeRecipes(setOf(a.id, b.id))

        assertEquals(listOf(c.id), repo.snapshot().map { it.id })
    }

    @Test
    fun ignoresUnknownIds() = runBlocking {
        val a = repo.addRecipe(NewRecipe(name = "Pasta", ingredients = listOf("200 g pasta")))

        repo.removeRecipes(setOf("missing-id"))

        assertEquals(listOf(a.id), repo.snapshot().map { it.id })
    }

    @Test
    fun emptySelectionIsANoop() = runBlocking {
        val a = repo.addRecipe(NewRecipe(name = "Pasta", ingredients = listOf("200 g pasta")))

        repo.removeRecipes(emptySet())

        assertEquals(listOf(a.id), repo.snapshot().map { it.id })
    }
}
