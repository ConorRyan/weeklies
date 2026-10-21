package com.conorryan.weeklies.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.conorryan.weeklies.AppGraph
import com.conorryan.weeklies.data.model.DayAssignments
import com.conorryan.weeklies.data.model.Recipe
import com.conorryan.weeklies.data.model.Weekday
import com.conorryan.weeklies.data.model.emptyByDay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlanUiState(
    val byDay: DayAssignments = emptyByDay(),
    val recipes: List<Recipe> = emptyList(),
    val today: Weekday = Weekday.today(),
)

class PlanViewModel(private val graph: AppGraph) : ViewModel() {

    val state: StateFlow<PlanUiState> = combine(
        graph.planRepository.byDay,
        graph.recipeRepository.recipes,
    ) { byDay, recipes ->
        PlanUiState(byDay = byDay, recipes = recipes, today = Weekday.today())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanUiState())

    fun setDayRecipe(day: Weekday, recipeId: String?) {
        viewModelScope.launch { graph.planRepository.setDayRecipe(day, recipeId) }
    }

    class Factory(private val graph: AppGraph) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = PlanViewModel(graph) as T
    }
}
