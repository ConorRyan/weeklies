package io.github.conorryan.weeklies.ui.import

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.data.import.RecipeImportErrorDisplay
import io.github.conorryan.weeklies.data.import.RecipeImportErrors
import io.github.conorryan.weeklies.data.model.NewRecipe
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ImportUiState(
    val url: String = "",
    val busy: Boolean = false,
    val error: RecipeImportErrorDisplay? = null,
    val preview: NewRecipe? = null,
)

class ImportViewModel(private val graph: AppGraph) : ViewModel() {

    private val _state = MutableStateFlow(ImportUiState())
    val state: StateFlow<ImportUiState> = _state.asStateFlow()

    private var job: Job? = null

    fun setUrl(url: String) {
        _state.update { it.copy(url = url, error = null) }
    }

    fun cancelImport() {
        job?.cancel()
        job = null
        _state.update { it.copy(busy = false) }
    }

    fun clearPreview() {
        _state.update { it.copy(preview = null) }
    }

    fun runScraperImport() {
        // URL shape is validated by the import pipeline (PageFetcher); only the
        // empty case gets its own copy here for instant feedback.
        if (state.value.url.isBlank()) {
            _state.update { it.copy(error = RecipeImportErrors.format(RecipeImportErrors.missingUrl())) }
            return
        }
        run { graph.recipeImporter.importRecipeFromUrlWithScraper(state.value.url.trim()) }
    }

    fun runAiImport(apiKey: String) {
        if (apiKey.isBlank()) {
            _state.update { it.copy(error = RecipeImportErrors.format(RecipeImportErrors.mistralKeyMissing())) }
            return
        }
        if (state.value.url.isBlank()) {
            _state.update { it.copy(error = RecipeImportErrors.format(RecipeImportErrors.missingUrl())) }
            return
        }
        run { graph.recipeImporter.importRecipeFromUrlWithMistral(state.value.url.trim(), apiKey.trim()) }
    }

    fun saveRecipe(recipe: NewRecipe) {
        viewModelScope.launch { graph.recipeRepository.addRecipe(recipe) }
    }

    private fun run(block: suspend () -> NewRecipe) {
        _state.update { it.copy(busy = true, error = null, preview = null) }
        job = viewModelScope.launch {
            try {
                val recipe = block()
                _state.update { it.copy(busy = false, preview = recipe) }
            } catch (e: kotlinx.coroutines.CancellationException) {
                _state.update { it.copy(busy = false) }
            } catch (e: Exception) {
                _state.update { it.copy(busy = false, error = RecipeImportErrors.format(e)) }
            }
        }
    }

    class Factory(private val graph: AppGraph) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ImportViewModel(graph) as T
    }
}
