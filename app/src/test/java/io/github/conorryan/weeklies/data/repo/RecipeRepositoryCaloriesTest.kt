package io.github.conorryan.weeklies.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.conorryan.weeklies.data.db.AppDatabase
import io.github.conorryan.weeklies.data.model.NewRecipe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Per-portion calorie estimates through the repository's interface: they
 * travel with [NewRecipe] on add and update, and round-trip through replaceAll.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RecipeRepositoryCaloriesTest {

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
    fun newRecipesStartWithoutCalories() = runBlocking {
        val recipe = repo.addRecipe(NewRecipe(name = "Pasta", ingredients = listOf("200 g pasta")))
        assertNull(recipe.calories)
    }

    @Test
    fun addRecipeStoresProvidedCalories() = runBlocking {
        val recipe = repo.addRecipe(NewRecipe(name = "Pasta", ingredients = listOf("200 g pasta"), calories = 550))

        assertEquals(550, repo.snapshot().single().calories)
    }

    @Test
    fun addRecipeIgnoresNonPositiveCalories() = runBlocking {
        val recipe = repo.addRecipe(NewRecipe(name = "Pasta", ingredients = listOf("200 g pasta"), calories = 0))

        assertNull(repo.snapshot().single().calories)
    }

    @Test
    fun updateRecipeKeepsCaloriesWhenTheDraftCarriesThem() = runBlocking {
        val recipe = repo.addRecipe(NewRecipe(name = "Pasta", ingredients = listOf("200 g pasta"), instructions = "Boil.", calories = 550))

        repo.updateRecipe(recipe.id, NewRecipe(name = "Pasta", ingredients = listOf("100 g pasta"), instructions = "Boil.", calories = 550))

        val after = repo.snapshot().single()
        assertEquals(listOf("100 g pasta"), after.ingredients)
        assertEquals(550, after.calories)
    }

    @Test
    fun updateRecipeClearsCaloriesWhenTheDraftHasNone() = runBlocking {
        val recipe = repo.addRecipe(NewRecipe(name = "Pasta", ingredients = listOf("200 g pasta"), calories = 550))

        repo.updateRecipe(recipe.id, NewRecipe(name = "Pasta", ingredients = listOf("100 g pasta")))

        assertNull(repo.snapshot().single().calories)
    }

    @Test
    fun updateRecipeIgnoresNonPositiveCalories() = runBlocking {
        val recipe = repo.addRecipe(NewRecipe(name = "Pasta", ingredients = listOf("200 g pasta")))

        repo.updateRecipe(recipe.id, NewRecipe(name = "Pasta", ingredients = listOf("100 g pasta"), calories = -5))

        assertNull(repo.snapshot().single().calories)
    }

    @Test
    fun replaceAllRoundTripsCalories() = runBlocking {
        val recipe = repo.addRecipe(NewRecipe(name = "Pasta", ingredients = listOf("200 g pasta"), calories = 550))
        val snapshot = repo.snapshot()

        repo.replaceAll(emptyList())
        assertEquals(0, repo.snapshot().size)

        repo.replaceAll(snapshot)
        assertEquals(550, repo.snapshot().single().calories)
    }
}
