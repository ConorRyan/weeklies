package io.github.conorryan.weeklies.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.R
import io.github.conorryan.weeklies.data.model.AppSettings
import io.github.conorryan.weeklies.data.model.DayAssignments
import io.github.conorryan.weeklies.data.model.Recipe
import io.github.conorryan.weeklies.data.model.Weekday
import io.github.conorryan.weeklies.data.model.emptyByDay
import io.github.conorryan.weeklies.ui.ToastBus
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanUiState(
    val byDay: DayAssignments = emptyByDay(),
    val recipes: List<Recipe> = emptyList(),
    val today: Weekday = Weekday.today(),
    val defaultPortions: Int = AppSettings.DEFAULT_PORTIONS_PER_DAY,
) {
    /** The recipe assigned to [day], or null when unassigned or since deleted. */
    fun recipeFor(day: Weekday): Recipe? = byDay[day]?.let { id -> recipes.firstOrNull { it.id == id } }

    /** The rest of the week in calendar order, starting the day after today. */
    val restOfWeek: List<Weekday>
        get() = (1 until Weekday.entries.size).map { Weekday.entries[(today.ordinal + it) % Weekday.entries.size] }
}

class PlanViewModel(
    private val graph: AppGraph,
    private val toastBus: ToastBus,
) : ViewModel() {

    val state: StateFlow<PlanUiState> = combine(
        graph.planRepository.byDay,
        graph.recipeRepository.recipes,
        graph.settingsStore.settings,
    ) { byDay, recipes, settings ->
        PlanUiState(
            byDay = byDay,
            recipes = recipes,
            today = Weekday.today(),
            defaultPortions = settings.portionsPerDay,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState())

    fun setDayRecipe(day: Weekday, recipeId: String?) {
        viewModelScope.launch { graph.planRepository.setDayRecipe(day, recipeId) }
    }

    /** Adds [recipe]'s ingredients for [portions] to the shopping list and confirms with a toast. */
    fun addToShoppingList(recipe: Recipe, portions: Int) {
        viewModelScope.launch {
            graph.shoppingRepository.addRecipeIngredients(recipe, portions)
            toastBus.info(R.string.plan_added_to_list, titleArg = recipe.name)
        }
    }

    class Factory(private val graph: AppGraph, private val toastBus: ToastBus) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PlanViewModel(graph, toastBus) as T
    }
}
