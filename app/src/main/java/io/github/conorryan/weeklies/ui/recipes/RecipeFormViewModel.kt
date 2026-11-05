package io.github.conorryan.weeklies.ui.recipes

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.R
import io.github.conorryan.weeklies.data.import.CalorieEstimate
import io.github.conorryan.weeklies.data.import.CalorieEstimator
import io.github.conorryan.weeklies.data.import.CleanedIngredients
import io.github.conorryan.weeklies.data.import.CleanupPairing
import io.github.conorryan.weeklies.data.import.applyCleanupSelection
import io.github.conorryan.weeklies.data.import.isUnchangedPairing
import io.github.conorryan.weeklies.data.import.pairCleanupForReview
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

/** Which AI feature is running, if any. */
enum class CleanupTarget { INGREDIENTS, INSTRUCTIONS, CALORIES }

/** Highest calorie value the form accepts for one portion. */
const val MAX_FORM_CALORIES = 9999

/** The recipe being written: name, editable ingredient rows, instructions, calories. */
data class RecipeDraft(
    val name: String = "",
    val ingredients: List<String> = listOf(""),
    val instructions: String = "",
    /** Raw kcal text for per-portion calories; blank means none. */
    val caloriesText: String = "",
)

data class RecipeFormUiState(
    val draft: RecipeDraft = RecipeDraft(),
    val hasApiKey: Boolean = false,
    val cleanupBusy: CleanupTarget? = null,
    @StringRes val validationError: Int? = null,
    val cleanupError: RecipeImportErrorDisplay? = null,
    val ingredientPreview: CleanedIngredients? = null,
    /** Review rows for [ingredientPreview]: original line(s) against cleaned line(s), in order. */
    val ingredientPairings: List<CleanupPairing> = emptyList(),
    /** Indices into [ingredientPairings] whose cleaned lines will be used. */
    val acceptedPairings: Set<Int> = emptySet(),
    val instructionPreview: String? = null,
    /** The AI calorie guess under review; only the number is ever saved. */
    val calorieEstimate: CalorieEstimate? = null,
) {
    /** Indices of the pairings the cleanup actually changed; the review lists only these. */
    val changedPairings: List<Int>
        get() = ingredientPairings.indices.filterNot { isUnchangedPairing(ingredientPairings[it]) }

    val hasIngredientText: Boolean get() = draft.ingredients.any { it.isNotBlank() }
    val hasInstructions: Boolean get() = draft.instructions.isNotBlank()
}

/**
 * Owns the recipe draft, its validation, and the AI interactions (ingredient
 * and instruction cleanup, per-portion calorie estimate): busy, error copy,
 * preview, apply. Screens render [state] and call the mutation functions; no
 * cleanup or error knowledge stays in composition.
 *
 * The draft persists through process death via [SavedStateHandle].
 */
class RecipeFormViewModel(
    private val savedStateHandle: SavedStateHandle,
    initial: NewRecipe?,
    mistralKey: Flow<String>,
    private val cleaner: RecipeCleaner?,
    private val calorieEstimator: CalorieEstimator?,
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

    /** Edits the per-portion calories text; blank means the recipe has none. */
    fun setCalories(value: String) {
        setDraft(state.value.draft.copy(caloriesText = value))
        // Typing over the offered guess dismisses its verdict card.
        if (value.trim() != (state.value.calorieEstimate?.calories?.toString() ?: "")) {
            _state.update { it.copy(calorieEstimate = null) }
        }
    }

    /**
     * Validates the draft and hands the trimmed recipe to [onReady]; invalid
     * drafts set [RecipeFormUiState.validationError] instead.
     */
    fun submit(onReady: (NewRecipe) -> Unit) {
        val draft = state.value.draft
        val name = draft.name.trim()
        val ingredients = draft.ingredients.map { it.trim() }.filter { it.isNotEmpty() }
        val calories = draft.caloriesText.trim()
        val caloriesValue = calories.toIntOrNull()?.takeIf { it in 1..MAX_FORM_CALORIES }
        when {
            name.isEmpty() -> _state.update { it.copy(validationError = R.string.recipe_name_required) }
            ingredients.isEmpty() -> _state.update { it.copy(validationError = R.string.recipe_ingredient_required) }
            calories.isNotEmpty() && caloriesValue == null ->
                _state.update { it.copy(validationError = R.string.calories_invalid) }
            else -> {
                _state.update { it.copy(validationError = null) }
                onReady(
                    NewRecipe(
                        name = name,
                        ingredients = ingredients,
                        instructions = draft.instructions.trim(),
                        calories = caloriesValue,
                    ),
                )
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

    fun cleanIngredients() = runCleanup(CleanupTarget.INGREDIENTS, cleaner != null) {
        val draft = state.value.draft
        val cleaned = requireCleaner().cleanIngredients(
            latestKey,
            draft.name.trim(),
            draftIngredientLines(),
        )
        val pairings = pairCleanupForReview(draftIngredientLines(), cleaned)
        _state.update {
            it.copy(
                ingredientPreview = cleaned,
                ingredientPairings = pairings,
                // Every suggested change starts accepted; the review lets the user opt out per line.
                acceptedPairings = pairings.indices.filterNot { i -> isUnchangedPairing(pairings[i]) }.toSet(),
            )
        }
    }

    fun cleanInstructions() = runCleanup(CleanupTarget.INSTRUCTIONS, cleaner != null) {
        val draft = state.value.draft
        val cleaned = requireCleaner().cleanInstructions(
            latestKey,
            draft.name.trim(),
            draft.ingredients.map { it.trim() }.filter { it.isNotEmpty() },
            draft.instructions.trim(),
        )
        _state.update { it.copy(instructionPreview = cleaned) }
    }

    /** Accepts or rejects one suggested change in the ingredient cleanup review. */
    fun setPairingAccepted(index: Int, accepted: Boolean) {
        _state.update {
            if (index !in it.ingredientPairings.indices) return@update it
            it.copy(acceptedPairings = if (accepted) it.acceptedPairings + index else it.acceptedPairings - index)
        }
    }

    fun acceptAllPairings() {
        _state.update { it.copy(acceptedPairings = it.changedPairings.toSet()) }
    }

    fun rejectAllPairings() {
        _state.update { it.copy(acceptedPairings = emptySet()) }
    }

    /** Applies the accepted changes; rejected ones keep the original lines. */
    fun applyIngredientCleanup() {
        val current = state.value
        if (current.ingredientPreview == null) return
        setDraft(current.draft.copy(ingredients = applyCleanupSelection(current.ingredientPairings, current.acceptedPairings)))
        dismissIngredientCleanup()
    }

    fun dismissIngredientCleanup() {
        _state.update { it.copy(ingredientPreview = null, ingredientPairings = emptyList(), acceptedPairings = emptySet()) }
    }

    private fun draftIngredientLines(): List<String> =
        state.value.draft.ingredients.map { it.trim() }.filter { it.isNotEmpty() }

    fun applyInstructionCleanup() {
        val preview = state.value.instructionPreview ?: return
        setDraft(state.value.draft.copy(instructions = preview))
        _state.update { it.copy(instructionPreview = null) }
    }

    fun dismissInstructionCleanup() {
        _state.update { it.copy(instructionPreview = null) }
    }

    // ---- Calorie estimate ------------------------------------------------------

    /**
     * Asks Mistral to estimate the draft's per-portion calories; the guess
     * fills the editable field right away, and its confidence and rationale
     * show under it until the user types something else. Only the number is
     * saved. Re-estimating replaces the previous guess.
     */
    fun estimateCalories() = runCleanup(CleanupTarget.CALORIES, calorieEstimator != null) {
        val draft = state.value.draft
        val estimate = requireEstimator().estimate(
            latestKey,
            draft.name.trim(),
            draftIngredientLines(),
            draft.instructions.trim(),
        )
        _state.update { it.copy(calorieEstimate = estimate) }
        setDraft(state.value.draft.copy(caloriesText = estimate.calories.toString()))
    }

    /** Clears the verdict card under the calories field; the number stays. */
    fun dismissCalorieEstimate() {
        _state.update { it.copy(calorieEstimate = null) }
    }

    private fun requireEstimator(): CalorieEstimator =
        calorieEstimator ?: throw RecipeImportErrors.mistralKeyMissing()

    fun dismissCleanupError() {
        _state.update { it.copy(cleanupError = null) }
    }

    private fun requireCleaner(): RecipeCleaner =
        cleaner ?: throw RecipeImportErrors.mistralKeyMissing()

    private fun runCleanup(target: CleanupTarget, available: Boolean, work: suspend () -> Unit) {
        if (!available || !state.value.hasApiKey) {
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
        savedStateHandle[KEY_CALORIES] = draft.caloriesText
        _state.update { it.copy(draft = draft) }
    }

    private fun restoreDraft(savedStateHandle: SavedStateHandle, initial: NewRecipe?): RecipeDraft {
        if (savedStateHandle.contains(KEY_NAME)) {
            return RecipeDraft(
                name = savedStateHandle.get<String>(KEY_NAME) ?: "",
                ingredients = savedStateHandle.get<ArrayList<String>>(KEY_INGREDIENTS) ?: listOf(""),
                instructions = savedStateHandle.get<String>(KEY_INSTRUCTIONS) ?: "",
                caloriesText = savedStateHandle.get<String>(KEY_CALORIES) ?: "",
            )
        }
        return RecipeDraft(
            name = initial?.name ?: "",
            ingredients = initial?.ingredients?.takeIf { it.isNotEmpty() } ?: listOf(""),
            instructions = initial?.instructions ?: "",
            caloriesText = initial?.calories?.toString() ?: "",
        )
    }

    companion object {
        private const val KEY_NAME = "draft.name"
        private const val KEY_INGREDIENTS = "draft.ingredients"
        private const val KEY_INSTRUCTIONS = "draft.instructions"
        private const val KEY_CALORIES = "draft.calories"

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
                    graph.calorieEstimator,
                    saveDraft,
                ) as T
        }
    }
}
