package io.github.conorryan.weeklies.ui.recipes

import io.github.conorryan.weeklies.data.model.Recipe
import io.github.conorryan.weeklies.data.model.Weekday
import io.github.conorryan.weeklies.data.model.emptyByDay
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure helpers behind the recipe screens: search, steps, planned days, scale presets. */
class RecipeDisplayTest {

    private val risotto = Recipe("1", "Mushroom risotto", listOf("75 g arborio rice", "60 g mushrooms"))
    private val crepes = Recipe("2", "Crêpes", listOf("100 g flour", "2 eggs"))
    private val all = listOf(risotto, crepes)

    @Test
    fun blankQueryReturnsEverything() {
        assertEquals(all, filterRecipes(all, "  "))
    }

    @Test
    fun matchesNameOrIngredientIgnoringCase() {
        assertEquals(listOf(risotto), filterRecipes(all, "RISOTTO"))
        assertEquals(listOf(crepes), filterRecipes(all, "flour"))
    }

    @Test
    fun matchesIgnoringAccents() {
        assertEquals(listOf(crepes), filterRecipes(all, "crepes"))
    }

    @Test
    fun everyWordMustMatch() {
        assertEquals(listOf(risotto), filterRecipes(all, "rice mushroom"))
        assertEquals(emptyList<Recipe>(), filterRecipes(all, "rice eggs"))
    }

    @Test
    fun stepsSplitOnLinesAndDropMarkers() {
        assertEquals(
            listOf("Soften the onion.", "Add the rice.", "Serve."),
            instructionSteps("1. Soften the onion.\n\n- Add the rice.\n3) Serve.\n"),
        )
    }

    @Test
    fun singleParagraphIsOneStep() {
        assertEquals(listOf("Boil, then serve."), instructionSteps("  Boil, then serve.  "))
        assertEquals(emptyList<String>(), instructionSteps("   "))
    }

    @Test
    fun plannedDaysListsAssignedDaysInWeekOrder() {
        val byDay = emptyByDay() + mapOf(Weekday.entries[4] to "1", Weekday.entries[0] to "1", Weekday.entries[2] to "2")
        assertEquals(listOf(Weekday.entries[0], Weekday.entries[4]), plannedDays(byDay, "1"))
    }

    @Test
    fun presetsRunFromDivideBySixToTimesSix() {
        assertEquals(listOf("÷6", "÷4", "÷2", "×2", "×4", "×6"), SCALE_PRESETS.map { it.label })
        assertEquals(0.25, SCALE_PRESETS[1].factor, 1e-9)
        assertEquals("×1.5", scaleLabel(1.5, divide = false))
    }
}
