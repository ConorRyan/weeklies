package io.github.conorryan.weeklies.ui.recipes

import androidx.lifecycle.SavedStateHandle
import io.github.conorryan.weeklies.data.import.CleanedIngredients
import io.github.conorryan.weeklies.data.import.HttpResponse
import io.github.conorryan.weeklies.data.import.HttpTransport
import io.github.conorryan.weeklies.data.import.RecipeCleaner
import io.github.conorryan.weeklies.data.import.RecipeImportErrorCode
import io.github.conorryan.weeklies.data.import.RecipeImportException
import io.github.conorryan.weeklies.data.model.NewRecipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** The whole draft, validation, and AI cleanup interaction through one interface. */
class RecipeFormViewModelTest {

    private class FakeTransport(vararg responses: HttpResponse) : HttpTransport {
        val requests = mutableListOf<String>()
        private val queue = responses.toMutableList()

        override suspend fun execute(
            method: String,
            url: String,
            headers: Map<String, String>,
            body: String?,
        ): HttpResponse {
            requests.add(body ?: "")
            check(queue.isNotEmpty()) { "no queued response" }
            return queue.removeAt(0)
        }
    }

    private fun chatResponse(content: String) = HttpResponse(
        status = 200,
        bodyText = """{"choices":[{"message":{"content":${kotlinx.serialization.json.JsonPrimitive(content)}}}]}""",
    )

    private val key = MutableStateFlow("")

    private fun vm(
        initial: NewRecipe? = null,
        cleaner: RecipeCleaner? = null,
        savedState: SavedStateHandle = SavedStateHandle(),
        save: suspend (NewRecipe) -> Unit = {},
    ) = RecipeFormViewModel(savedState, initial, key, cleaner, save)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ---- Draft ----------------------------------------------------------------

    @Test
    fun newRecipeDraftStartsWithOneEmptyIngredientRow() {
        val vm = vm(initial = null)

        assertEquals(RecipeDraft(name = "", ingredients = listOf(""), instructions = ""), vm.state.value.draft)
    }

    @Test
    fun editDraftStartsFromInitialRecipe() {
        val vm = vm(initial = NewRecipe("Pasta", listOf("200 g pasta"), "Boil."))

        assertEquals(
            RecipeDraft(name = "Pasta", ingredients = listOf("200 g pasta"), instructions = "Boil."),
            vm.state.value.draft,
        )
    }

    @Test
    fun draftEditsFlowThroughSetters() {
        val vm = vm()

        vm.setName(" Soup ")
        vm.setIngredient(0, "2 carrots")
        vm.addIngredient()
        vm.setIngredient(1, "")
        vm.setIngredient(1, "1 onion")
        vm.setInstructions(" Chop. ")

        val draft = vm.state.value.draft
        assertEquals(" Soup ", draft.name)
        assertEquals(listOf("2 carrots", "1 onion"), draft.ingredients)
        assertEquals(" Chop. ", draft.instructions)
    }

    @Test
    fun removeIngredientKeepsAtLeastOneRow() {
        val vm = vm(initial = NewRecipe("Pasta", listOf("a")))

        vm.removeIngredient(0)

        assertEquals(listOf("a"), vm.state.value.draft.ingredients)
    }

    @Test
    fun draftRestoresFromSavedStateHandle() {
        val savedState = SavedStateHandle(
            mapOf(
                "draft.name" to "Soup",
                "draft.ingredients" to arrayListOf("2 carrots"),
                "draft.instructions" to "Boil.",
            ),
        )
        val vm = vm(initial = NewRecipe("Other", listOf("x")), savedState = savedState)

        assertEquals(RecipeDraft("Soup", listOf("2 carrots"), "Boil."), vm.state.value.draft)
    }

    @Test
    fun settersPersistDraftIntoSavedStateHandle() {
        val savedState = SavedStateHandle()
        val vm = vm(savedState = savedState)

        vm.setName("Soup")
        vm.setIngredient(0, "2 carrots")

        assertEquals("Soup", savedState.get<String>("draft.name"))
        assertEquals(arrayListOf("2 carrots"), savedState.get<ArrayList<String>>("draft.ingredients"))
    }

    // ---- Validation -----------------------------------------------------------

    @Test
    fun submitRejectsEmptyName() {
        val vm = vm()
        var submitted: NewRecipe? = null

        vm.submit { submitted = it }

        assertEquals("Recipe name is required.", vm.state.value.validationError)
        assertNull(submitted)
    }

    @Test
    fun submitRejectsBlankIngredients() {
        val vm = vm()
        vm.setName("Pasta")

        vm.submit { }

        assertEquals("Add at least one ingredient.", vm.state.value.validationError)
    }

    @Test
    fun submitHandsBackTrimmedRecipeAndClearsError() {
        val vm = vm(initial = NewRecipe("Pasta", listOf("pasta")))
        vm.setName(" Pasta ")
        vm.setIngredient(0, " 200 g pasta ")
        vm.addIngredient()
        vm.submit { } // sets an error first
        vm.setIngredient(1, "1 onion")
        var submitted: NewRecipe? = null

        vm.submit { submitted = it }

        assertEquals(NewRecipe("Pasta", listOf("200 g pasta", "1 onion")), submitted)
        assertNull(vm.state.value.validationError)
    }

    @Test
    fun saveAwaitsStoreThenReportsBack() = runTest {
        val saved = mutableListOf<NewRecipe>()
        var savedCalled = false
        val vm = vm(save = { saved.add(it) })

        vm.save(NewRecipe("Pasta", listOf("200 g pasta"))) { savedCalled = true }

        assertEquals(listOf(NewRecipe("Pasta", listOf("200 g pasta"))), saved)
        assertTrue(savedCalled)
    }

    // ---- AI cleanup -----------------------------------------------------------

    @Test
    fun cleanIngredientsWithoutKeyShowsErrorAndCallsNothing() {
        val transport = FakeTransport()
        val vm = vm(cleaner = RecipeCleaner(transport))
        vm.setName("Pasta")
        vm.setIngredient(0, "200 g pasta")

        vm.cleanIngredients()

        val error = vm.state.value.cleanupError
        assertNotNull(error)
        assertTrue(transport.requests.isEmpty())
    }

    @Test
    fun cleanIngredientsProducesPreviewThenApplyReplacesDraft() {
        val transport = FakeTransport(
            chatResponse("""{"ingredients":[{"name":"pasta","quantity":"400 g","from":[0]}]}"""),
        )
        val vm = vm(cleaner = RecipeCleaner(transport))
        vm.setName("Pasta")
        vm.setIngredient(0, "200 g pasta")
        key.value = "k-1"

        vm.cleanIngredients()

        assertEquals(
            CleanedIngredients(lines = listOf("400 g pasta"), from = listOf(listOf(0))),
            vm.state.value.ingredientPreview,
        )
        assertEquals(listOf("200 g pasta"), vm.state.value.draft.ingredients)

        vm.applyIngredientCleanup()

        assertEquals(listOf("400 g pasta"), vm.state.value.draft.ingredients)
        assertNull(vm.state.value.ingredientPreview)
    }

    @Test
    fun dismissIngredientCleanupDropsPreview() {
        val transport = FakeTransport(
            chatResponse("""{"ingredients":[{"name":"pasta","quantity":"400 g","from":[0]}]}"""),
        )
        val vm = vm(cleaner = RecipeCleaner(transport))
        vm.setIngredient(0, "200 g pasta")
        key.value = "k-1"
        vm.cleanIngredients()

        vm.dismissIngredientCleanup()

        assertNull(vm.state.value.ingredientPreview)
        assertEquals(listOf("200 g pasta"), vm.state.value.draft.ingredients)
    }

    @Test
    fun cleanInstructionsProducesPreviewThenApplyReplacesInstructions() {
        val transport = FakeTransport(chatResponse("""{"instructions":"- Boil\n- Serve"}"""))
        val vm = vm(cleaner = RecipeCleaner(transport))
        vm.setName("Pasta")
        vm.setIngredient(0, "pasta")
        vm.setInstructions("Boil water. Serve.")
        key.value = "k-1"

        vm.cleanInstructions()

        assertEquals("- Boil\n- Serve", vm.state.value.instructionPreview)

        vm.applyInstructionCleanup()

        assertEquals("- Boil\n- Serve", vm.state.value.draft.instructions)
        assertNull(vm.state.value.instructionPreview)
    }

    @Test
    fun cleanupFailureShowsErrorAndClearsBusy() {
        val transport = FakeTransport(HttpResponse(500, "{}"))
        val vm = vm(cleaner = RecipeCleaner(transport))
        vm.setIngredient(0, "200 g pasta")
        key.value = "k-1"

        vm.cleanIngredients()

        assertNull(vm.state.value.cleanupBusy)
        assertNull(vm.state.value.ingredientPreview)
        val error = vm.state.value.cleanupError
        assertNotNull(error)

        vm.dismissCleanupError()
        assertNull(vm.state.value.cleanupError)
    }

    @Test
    fun missingCleanerSurfacesKeyError() {
        val vm = vm(cleaner = null)
        vm.setIngredient(0, "200 g pasta")
        key.value = "k-1"

        vm.cleanIngredients()

        assertNotNull(vm.state.value.cleanupError)
    }

}

