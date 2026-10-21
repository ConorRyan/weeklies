package io.github.conorryan.weeklies.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.data.import.scaleIngredientLine
import io.github.conorryan.weeklies.data.model.AppSettings
import io.github.conorryan.weeklies.data.model.Recipe
import io.github.conorryan.weeklies.data.model.ShoppingItem
import io.github.conorryan.weeklies.data.model.Weekday
import io.github.conorryan.weeklies.ui.ToastBus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShoppingUiState(
    val items: List<ShoppingItem> = emptyList(),
    val hasChecked: Boolean = false,
    val recipes: List<Recipe> = emptyList(),
    val todayRecipeId: String? = null,
    val defaultPortions: Int = AppSettings.DEFAULT_PORTIONS_PER_DAY,
)

class ShoppingViewModel(
    private val graph: AppGraph,
    private val toastBus: ToastBus,
) : ViewModel() {

    val state: StateFlow<ShoppingUiState> = combine(
        graph.shoppingRepository.items,
        graph.recipeRepository.recipes,
        graph.planRepository.byDay,
        graph.settingsStore.settings,
    ) { items, recipes, byDay, settings ->
        ShoppingUiState(
            items = items,
            hasChecked = items.any { it.checked },
            recipes = recipes,
            todayRecipeId = byDay[Weekday.today()],
            defaultPortions = settings.portionsPerDay,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingUiState())

    fun tick(id: String) {
        viewModelScope.launch { graph.shoppingRepository.toggleItem(id) }
    }

    fun addItem(label: String) {
        viewModelScope.launch { graph.shoppingRepository.addItem(label) }
    }

    fun renameItem(id: String, label: String) {
        viewModelScope.launch { graph.shoppingRepository.renameItem(id, label) }
    }

    fun removeChecked() {
        viewModelScope.launch { graph.shoppingRepository.removeCheckedItems() }
    }

    /** Adds the recipe's ingredients, scaled to [portions]; toasts if the recipe is gone. */
    fun addRecipeIngredients(recipeId: String, portions: Int) {
        viewModelScope.launch {
            val recipe = graph.recipeRepository.snapshot().firstOrNull { it.id == recipeId }
            if (recipe == null) {
                toastBus.info(
                    "Recipe missing",
                    "The recipe was removed. Update Weeklies or Recipes.",
                )
            } else {
                val factor = portions.toDouble()
                for (line in recipe.ingredients) {
                    val trimmed = line.trim()
                    if (trimmed.isEmpty()) continue
                    graph.shoppingRepository.addItem(scaleIngredientLine(trimmed, factor))
                }
            }
        }
    }

    class Factory(private val graph: AppGraph, private val toastBus: ToastBus) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ShoppingViewModel(graph, toastBus) as T
    }
}
