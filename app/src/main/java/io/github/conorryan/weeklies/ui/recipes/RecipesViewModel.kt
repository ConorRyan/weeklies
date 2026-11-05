package io.github.conorryan.weeklies.ui.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.R
import io.github.conorryan.weeklies.data.model.Recipe
import io.github.conorryan.weeklies.data.model.NewRecipe
import io.github.conorryan.weeklies.data.repo.RecipeRepository
import io.github.conorryan.weeklies.data.repo.ShoppingRepository
import io.github.conorryan.weeklies.ui.ToastBus
import kotlinx.coroutines.launch

/**
 * Tab-level recipe operations for the Recipes tab and the recipe detail
 * screen: deleting a recipe and rescaling its amounts, so repository writes
 * no longer happen from composable scopes.
 */
class RecipesViewModel(
    private val recipeRepository: RecipeRepository,
    private val shoppingRepository: ShoppingRepository,
    private val toastBus: ToastBus? = null,
) : ViewModel() {

    fun removeRecipe(id: String) {
        viewModelScope.launch { recipeRepository.removeRecipe(id) }
    }

    /** Deletes every selected recipe in one repository operation. */
    fun removeRecipes(ids: Set<String>) {
        if (ids.isEmpty()) return
        viewModelScope.launch { recipeRepository.removeRecipes(ids) }
    }

    /** Replaces the recipe's ingredient lines with [scaled], keeping name and instructions. */
    fun rescaleRecipe(recipeId: String, scaled: List<String>) {
        viewModelScope.launch {
            val current = recipeRepository.snapshot().firstOrNull { it.id == recipeId } ?: return@launch
            recipeRepository.updateRecipe(
                recipeId,
                NewRecipe(name = current.name, ingredients = scaled, instructions = current.instructions, calories = current.calories),
            )
        }
    }

    /** Adds [recipe]'s ingredients for [portions] to the shopping list and confirms with a toast. */
    fun addToShoppingList(recipe: Recipe, portions: Int) {
        viewModelScope.launch {
            shoppingRepository.addRecipeIngredients(recipe, portions)
            toastBus?.info(R.string.plan_added_to_list, titleArg = recipe.name)
        }
    }

    class Factory(private val graph: AppGraph, private val toastBus: ToastBus? = null) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = RecipesViewModel(
            graph.recipeRepository,
            graph.shoppingRepository,
            toastBus,
        ) as T
    }
}
