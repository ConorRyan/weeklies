package com.conorryan.weeklies.data.import

import org.junit.Assert.assertEquals
import org.junit.Test

class ScaleIngredientLineTest {

    @Test
    fun returnsLineUnchangedWhenFactorIsOne() {
        assertEquals("2 cups flour", scaleIngredientLine("2 cups flour", 1.0))
    }

    @Test
    fun returnsLineUnchangedWhenFactorIsNotFinite() {
        assertEquals("2 cups flour", scaleIngredientLine("2 cups flour", Double.NaN))
    }

    @Test
    fun scalesIntegers() {
        assertEquals("4 cups flour", scaleIngredientLine("2 cups flour", 2.0))
    }

    @Test
    fun scalesDecimals() {
        assertEquals("3 tsp salt", scaleIngredientLine("1.5 tsp salt", 2.0))
    }

    @Test
    fun scalesFractionsWithoutSpaces() {
        assertEquals("1 cup sugar", scaleIngredientLine("1/2 cup sugar", 2.0))
    }

    @Test
    fun scalesFractionsWithSpaces() {
        assertEquals("1 cup sugar", scaleIngredientLine("1 / 2 cup sugar", 2.0))
    }

    @Test
    fun leavesNonNumericTokensUntouched() {
        assertEquals("salt to taste", scaleIngredientLine("salt to taste", 2.0))
    }

    @Test
    fun scalesMultipleNumericTokens() {
        assertEquals("2 1 cups flour", scaleIngredientLine("1 1/2 cups flour", 2.0))
    }

    @Test
    fun leavesZeroDenominatorFractionsUntouched() {
        assertEquals("1/0 cups", scaleIngredientLine("1/0 cups", 2.0))
    }

    @Test
    fun roundsRepeatingDecimalsToFourPlaces() {
        assertEquals("0.3333 cup", scaleIngredientLine("1 cup", 1.0 / 3.0))
    }
}
