package io.github.conorryan.weeklies.data.repo

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.conorryan.weeklies.data.db.AppDatabase
import io.github.conorryan.weeklies.data.model.Recipe
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The recipe-to-shopping flow through the repository's interface: strip
 * notes, scale amounts, merge duplicates, one transaction.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ShoppingRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repo: ShoppingRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = ShoppingRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun labels(): List<String> = runBlocking { repo.snapshot().map { it.label } }

    private fun recipe(vararg ingredients: String) =
        Recipe(id = "r1", name = "Pasta", ingredients = ingredients.toList())

    @Test
    fun addRecipeIngredientsStripsNotesAndScalesByPortions() = runBlocking {
        repo.addRecipeIngredients(
            recipe("250 g mushrooms (trimmed)", "1 onion (finely chopped)", "(to taste)"),
            portions = 2,
        )

        assertEquals(listOf("500 g mushrooms", "2 onion"), labels())
    }

    @Test
    fun addRecipeIngredientsMergesIntoMatchingUncheckedLine() = runBlocking {
        repo.addItem("100g pasta")
        repo.addRecipeIngredients(recipe("100 g pasta"), portions = 1)

        assertEquals(listOf("200g pasta"), labels())
    }

    @Test
    fun addRecipeIngredientsMergesScaledAmountsIntoExistingLine() = runBlocking {
        repo.addItem("100g pasta")
        repo.addRecipeIngredients(recipe("100 g pasta"), portions = 3)

        assertEquals(listOf("400g pasta"), labels())
    }

    @Test
    fun addRecipeIngredientsIgnoresCheckedLinesWhenMerging() = runBlocking {
        repo.addItem("100g pasta")
        val first = repo.snapshot().single()
        repo.toggleItem(first.id)
        repo.addRecipeIngredients(recipe("100 g pasta"), portions = 1)

        assertEquals(listOf("100g pasta", "100 g pasta"), labels())
    }

    @Test
    fun addRecipeIngredientsMergesDuplicatesInsideOneRecipe() = runBlocking {
        repo.addRecipeIngredients(recipe("100g pasta", "100 g pasta"), portions = 1)

        assertEquals(listOf("200g pasta"), labels())
    }

    @Test
    fun addRecipeIngredientsKeepsInsertOrderForNewLines() = runBlocking {
        repo.addRecipeIngredients(recipe("1 onion", "2 carrot"), portions = 1)

        assertEquals(listOf("1 onion", "2 carrot"), labels())
    }

    @Test
    fun addRecipeIngredientsIsNoOpForNoteOnlyIngredients() = runBlocking {
        repo.addRecipeIngredients(recipe("(to taste)", "  "), portions = 4)

        assertEquals(emptyList<String>(), labels())
    }
}
