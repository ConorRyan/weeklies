package io.github.conorryan.weeklies.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.data.model.NewRecipe
import kotlinx.coroutines.launch

/**
 * Tab-level recipe operations for the Recipes tab and the recipe detail
 * screen: deleting a recipe and rescaling its amounts, so repository writes
 * no longer happen from composable scopes.
 */
class RecipesViewModel(private val graph: AppGraph) : ViewModel() {

    fun removeRecipe(id: String) {
        viewModelScope.launch { graph.recipeRepository.removeRecipe(id) }
    }

    /** Replaces the recipe's ingredient lines with [scaled], keeping name and instructions. */
    fun rescaleRecipe(recipeId: String, scaled: List<String>) {
        viewModelScope.launch {
            val current = graph.recipeRepository.snapshot().firstOrNull { it.id == recipeId } ?: return@launch
            graph.recipeRepository.updateRecipe(
                recipeId,
                NewRecipe(name = current.name, ingredients = scaled, instructions = current.instructions),
            )
        }
    }

    class Factory(private val graph: AppGraph) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RecipesViewModel(graph) as T
    }
}
