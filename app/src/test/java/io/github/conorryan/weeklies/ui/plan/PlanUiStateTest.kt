package io.github.conorryan.weeklies.ui.plan

import io.github.conorryan.weeklies.data.model.Recipe
import io.github.conorryan.weeklies.data.model.Weekday
import io.github.conorryan.weeklies.data.model.emptyByDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The Week screen's derived values: the rest of the week and per-day recipe lookup. */
class PlanUiStateTest {

    private val days = Weekday.entries

    @Test
    fun restOfWeekStartsTomorrowAndWrapsAround() {
        val state = PlanUiState(today = days.last())

        assertEquals(days.dropLast(1), state.restOfWeek)
    }

    @Test
    fun restOfWeekFromMidweek() {
        val state = PlanUiState(today = days[2])

        assertEquals(days.drop(3) + days.take(2), state.restOfWeek)
        assertEquals(6, state.restOfWeek.size)
    }

    @Test
    fun recipeForResolvesAssignedRecipeAndIgnoresDeletedOnes() {
        val pasta = Recipe("p", "Pasta", listOf("200 g pasta"))
        val state = PlanUiState(
            byDay = emptyByDay() + mapOf(days[0] to "p", days[1] to "gone"),
            recipes = listOf(pasta),
        )

        assertEquals(pasta, state.recipeFor(days[0]))
        assertNull(state.recipeFor(days[1]))
        assertNull(state.recipeFor(days[2]))
    }
}
