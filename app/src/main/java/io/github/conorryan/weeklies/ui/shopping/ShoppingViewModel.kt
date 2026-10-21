package io.github.conorryan.weeklies.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.data.import.scaleIngredientLine
import io.github.conorryan.weeklies.data.model.ShoppingItem
import io.github.conorryan.weeklies.data.model.Weekday
import io.github.conorryan.weeklies.ui.ToastBus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ShoppingUiState(
    val items: List<ShoppingItem> = emptyList(),
    val hasChecked: Boolean = false,
)

class ShoppingViewModel(
    private val graph: AppGraph,
    private val toastBus: ToastBus,
) : ViewModel() {

    val state: StateFlow<ShoppingUiState> = graph.shoppingRepository.items
        .map { items -> ShoppingUiState(items = items, hasChecked = items.any { it.checked }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingUiState())

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

    /** Resolves today's planned recipe and adds its ingredients, scaled to portions per day. */
    fun addTodaysIngredients() {
        viewModelScope.launch {
            val recipeId = graph.planRepository.snapshot()[Weekday.today()]
            when (recipeId) {
                null -> toastBus.info(
                    "No recipe today",
                    "Assign a recipe to today on the Weeklies tab first.",
                )
                else -> {
                    val recipe = graph.recipeRepository.snapshot().firstOrNull { it.id == recipeId }
                    if (recipe == null) {
                        toastBus.info(
                            "Recipe missing",
                            "The planned recipe was removed. Update Weeklies or Recipes.",
                        )
                    } else {
                        val portions = graph.settingsStore.current().portionsPerDay.toDouble()
                        for (line in recipe.ingredients) {
                            val trimmed = line.trim()
                            if (trimmed.isEmpty()) continue
                            graph.shoppingRepository.addItem(scaleIngredientLine(trimmed, portions))
                        }
                    }
                }
            }
        }
    }

    class Factory(private val graph: AppGraph, private val toastBus: ToastBus) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ShoppingViewModel(graph, toastBus) as T
    }
}
