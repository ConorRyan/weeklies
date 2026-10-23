package io.github.conorryan.weeklies.ui.recipes

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.data.import.CleanedIngredients
import io.github.conorryan.weeklies.data.import.RecipeCleaner
import io.github.conorryan.weeklies.data.import.RecipeImportErrorDisplay
import io.github.conorryan.weeklies.data.import.RecipeImportErrors
import io.github.conorryan.weeklies.data.model.NewRecipe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Which AI cleanup is running, if any. */
enum class CleanupTarget { INGREDIENTS, INSTRUCTIONS }

/** The recipe being written: name, editable ingredient rows, instructions. */
data class RecipeDraft(
    val name: String = "",
    val ingredients: List<String> = listOf(""),
    val instructions: String = "",
)

data class RecipeFormUiState(
    val draft: RecipeDraft = RecipeDraft(),
    val hasApiKey: Boolean = false,
    val cleanupBusy: CleanupTarget? = null,
    val validationError: String? = null,
    val cleanupError: RecipeImportErrorDisplay? = null,
    val ingredientPreview: CleanedIngredients? = null,
    val instructionPreview: String? = null,
) {
    val hasIngredientText: Boolean get() = draft.ingredients.any { it.isNotBlank() }
    val hasInstructions: Boolean get() = draft.instructions.isNotBlank()
}

/**
 * Owns the recipe draft, its validation, and the AI cleanup interaction
 * (busy, error copy, preview, apply). Screens render [state] and call the
 * mutation functions; no cleanup or error knowledge stays in composition.
 *
 * The draft persists through process death via [SavedStateHandle].
 */
class RecipeFormViewModel(
    private val savedStateHandle: SavedStateHandle,
    initial: NewRecipe?,
    mistralKey: Flow<String>,
    private val cleaner: RecipeCleaner?,
    private val saveDraft: suspend (NewRecipe) -> Unit,
) : ViewModel() {

    private val _state = MutableStateFlow(RecipeFormUiState(draft = restoreDraft(savedStateHandle, initial)))
    val state: StateFlow<RecipeFormUiState> = _state.asStateFlow()

    /** Latest Mistral key; the cleanup calls need it, state only exposes presence. */
    private var latestKey: String = ""

    init {
        viewModelScope.launch {
            mistralKey.collect { key ->
                latestKey = key
                _state.update { it.copy(hasApiKey = key.isNotBlank()) }
            }
        }
    }

    // ---- Draft -------------------------------------------------------------

    fun setName(value: String) = setDraft(state.value.draft.copy(name = value))

    fun setIngredient(index: Int, value: String) {
        val ingredients = state.value.draft.ingredients.toMutableList()
        if (index in ingredients.indices) ingredients[index] = value
        setDraft(state.value.draft.copy(ingredients = ingredients))
    }

    fun addIngredient() =
        setDraft(state.value.draft.copy(ingredients = state.value.draft.ingredients + ""))

    fun removeIngredient(index: Int) {
        val current = state.value.draft.ingredients
        if (current.size <= 1) return
        setDraft(state.value.draft.copy(ingredients = current.filterIndexed { i, _ -> i != index }))
    }

    fun setInstructions(value: String) =
        setDraft(state.value.draft.copy(instructions = value))

    /**
     * Validates the draft and hands the trimmed recipe to [onReady]; invalid
     * drafts set [RecipeFormUiState.validationError] instead.
     */
    fun submit(onReady: (NewRecipe) -> Unit) {
        val draft = state.value.draft
        val name = draft.name.trim()
        val ingredients = draft.ingredients.map { it.trim() }.filter { it.isNotEmpty() }
        when {
            name.isEmpty() -> _state.update { it.copy(validationError = "Recipe name is required.") }
            ingredients.isEmpty() -> _state.update { it.copy(validationError = "Add at least one ingredient.") }
            else -> {
                _state.update { it.copy(validationError = null) }
                onReady(NewRecipe(name = name, ingredients = ingredients, instructions = draft.instructions.trim()))
            }
        }
    }

    /** Saves the recipe, then reports back (screens pop on success). */
    fun save(recipe: NewRecipe, onSaved: () -> Unit) {
        viewModelScope.launch {
            saveDraft(recipe)
            onSaved()
        }
    }

    // ---- AI cleanup ----------------------------------------------------------

    fun cleanIngredients() = runCleanup(CleanupTarget.INGREDIENTS) {
        val draft = state.value.draft
        val cleaned = requireCleaner().cleanIngredients(
            latestKey,
            draft.name.trim(),
            draft.ingredients.map { it.trim() }.filter { it.isNotEmpty() },
        )
        _state.update { it.copy(ingredientPreview = cleaned) }
    }

    fun cleanInstructions() = runCleanup(CleanupTarget.INSTRUCTIONS) {
        val draft = state.value.draft
        val cleaned = requireCleaner().cleanInstructions(
            latestKey,
            draft.name.trim(),
            draft.ingredients.map { it.trim() }.filter { it.isNotEmpty() },
            draft.instructions.trim(),
        )
        _state.update { it.copy(instructionPreview = cleaned) }
    }

    fun applyIngredientCleanup() {
        val preview = state.value.ingredientPreview ?: return
        setDraft(state.value.draft.copy(ingredients = preview.lines))
        _state.update { it.copy(ingredientPreview = null) }
    }

    fun dismissIngredientCleanup() {
        _state.update { it.copy(ingredientPreview = null) }
    }

    fun applyInstructionCleanup() {
        val preview = state.value.instructionPreview ?: return
        setDraft(state.value.draft.copy(instructions = preview))
        _state.update { it.copy(instructionPreview = null) }
    }

    fun dismissInstructionCleanup() {
        _state.update { it.copy(instructionPreview = null) }
    }

    fun dismissCleanupError() {
        _state.update { it.copy(cleanupError = null) }
    }

    private fun requireCleaner(): RecipeCleaner =
        cleaner ?: throw RecipeImportErrors.mistralKeyMissing()

    private fun runCleanup(target: CleanupTarget, work: suspend () -> Unit) {
        if (cleaner == null || !state.value.hasApiKey) {
            _state.update { it.copy(cleanupError = RecipeImportErrors.format(RecipeImportErrors.mistralKeyMissing())) }
            return
        }
        _state.update { it.copy(cleanupBusy = target) }
        viewModelScope.launch {
            try {
                work()
            } catch (e: CancellationException) {
                // canceled; nothing to show
            } catch (e: Exception) {
                _state.update { it.copy(cleanupError = RecipeImportErrors.format(e)) }
            } finally {
                _state.update { it.copy(cleanupBusy = null) }
            }
        }
    }

    // ---- Draft persistence ---------------------------------------------------

    private fun setDraft(draft: RecipeDraft) {
        savedStateHandle[KEY_NAME] = draft.name
        savedStateHandle[KEY_INGREDIENTS] = ArrayList(draft.ingredients)
        savedStateHandle[KEY_INSTRUCTIONS] = draft.instructions
        _state.update { it.copy(draft = draft) }
    }

    private fun restoreDraft(savedStateHandle: SavedStateHandle, initial: NewRecipe?): RecipeDraft {
        if (savedStateHandle.contains(KEY_NAME)) {
            return RecipeDraft(
                name = savedStateHandle.get<String>(KEY_NAME) ?: "",
                ingredients = savedStateHandle.get<ArrayList<String>>(KEY_INGREDIENTS) ?: listOf(""),
                instructions = savedStateHandle.get<String>(KEY_INSTRUCTIONS) ?: "",
            )
        }
        return RecipeDraft(
            name = initial?.name ?: "",
            ingredients = initial?.ingredients?.takeIf { it.isNotEmpty() } ?: listOf(""),
            instructions = initial?.instructions ?: "",
        )
    }

    companion object {
        private const val KEY_NAME = "draft.name"
        private const val KEY_INGREDIENTS = "draft.ingredients"
        private const val KEY_INSTRUCTIONS = "draft.instructions"

        /** Form for creating a new recipe. */
        fun newRecipeFactory(graph: AppGraph): ViewModelProvider.Factory =
            formFactory(graph, initial = null) { recipe -> graph.recipeRepository.addRecipe(recipe) }

        /** Form for editing the recipe with [recipeId]; [initial] seeds the draft. */
        fun editRecipeFactory(graph: AppGraph, recipeId: String, initial: NewRecipe?): ViewModelProvider.Factory =
            formFactory(graph, initial) { recipe -> graph.recipeRepository.updateRecipe(recipeId, recipe) }

        /** Form for reviewing an imported recipe before saving it. */
        fun importReviewFactory(graph: AppGraph, initial: NewRecipe): ViewModelProvider.Factory =
            formFactory(graph, initial) { recipe -> graph.recipeRepository.addRecipe(recipe) }

        private fun formFactory(
            graph: AppGraph,
            initial: NewRecipe?,
            saveDraft: suspend (NewRecipe) -> Unit,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                RecipeFormViewModel(
                    extras.createSavedStateHandle(),
                    initial,
                    graph.secretsStore.mistralApiKey,
                    graph.recipeCleaner,
                    saveDraft,
                ) as T
        }
    }
}
