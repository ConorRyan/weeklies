package io.github.conorryan.weeklies.data.import

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeServingsTest {

    @Test
    fun acceptsPositiveFiniteServings() {
        assertTrue(isValidServings(4.0))
    }

    @Test
    fun rejectsZeroAndNegativeServings() {
        assertFalse(isValidServings(0.0))
        assertFalse(isValidServings(-1.0))
    }

    @Test
    fun rejectsNonFiniteServings() {
        assertFalse(isValidServings(Double.NaN))
    }

    @Test
    fun scalesIngredientsFromFourServingsToOne() {
        val result = scaleIngredientsToServings(listOf("2 cups flour"), 4.0, 1.0)
        assertEquals("0.5 cups flour", result[0])
    }

    @Test
    fun returnsUnchangedWhenServingsInvalid() {
        val ingredients = listOf("2 cups flour")
        assertEquals(ingredients, scaleIngredientsToServings(ingredients, 0.0, 1.0))
    }

    @Test
    fun rescalesMultiPortionAmountsToSinglePortion() {
        assertEquals(
            listOf("100 g pasta"),
            rescaleToSinglePortion(listOf("400 g pasta"), 4.0),
        )
    }

    @Test
    fun rescaleMultipliesFractionalPortionAmounts() {
        assertEquals(
            listOf("2 cup flour"),
            rescaleToSinglePortion(listOf("1 cup flour"), 0.5),
        )
    }

    @Test
    fun rescaleReturnsUnchangedForInvalidPortions() {
        assertEquals(
            listOf("400 g pasta"),
            rescaleToSinglePortion(listOf("400 g pasta"), 0.0),
        )
        assertEquals(
            listOf("400 g pasta"),
            rescaleToSinglePortion(listOf("400 g pasta"), Double.NaN),
        )
    }

    @Test
    fun rescaleHandlesMixedNumbers() {
        assertEquals(
            listOf("0.375 cups flour"),
            rescaleToSinglePortion(listOf("1 1/2 cups flour"), 4.0),
        )
    }
}
