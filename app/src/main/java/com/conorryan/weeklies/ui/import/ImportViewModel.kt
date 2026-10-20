package com.conorryan.weeklies.ui.import

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.conorryan.weeklies.AppGraph
import com.conorryan.weeklies.data.import.RecipeImportErrorDisplay
import com.conorryan.weeklies.data.import.RecipeImportException
import com.conorryan.weeklies.data.import.RecipeImportErrors
import com.conorryan.weeklies.data.import.isValidRecipeUrl
import com.conorryan.weeklies.data.model.NewRecipe
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
        val url = state.value.url.trim()
        val inputError = when {
            url.isEmpty() -> RecipeImportErrors.missingUrl()
            !isValidRecipeUrl(url) -> RecipeImportErrors.invalidUrl()
            else -> null
        }
        if (inputError != null) {
            _state.update { it.copy(error = RecipeImportErrors.format(inputError)) }
            return
        }
        run { graph.recipeImporter.importRecipeFromUrlWithScraper(url) }
    }

    fun runAiImport(apiKey: String) {
        val key = apiKey.trim()
        val url = state.value.url.trim()
        val inputError = when {
            key.isEmpty() -> RecipeImportErrors.mistralKeyMissing()
            url.isEmpty() -> RecipeImportErrors.missingUrl()
            !isValidRecipeUrl(url) -> RecipeImportErrors.invalidUrl()
            else -> null
        }
        if (inputError != null) {
            _state.update { it.copy(error = RecipeImportErrors.format(inputError)) }
            return
        }
        run { graph.recipeImporter.importRecipeFromUrlWithMistral(url, key) }
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
