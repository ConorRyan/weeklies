package io.github.conorryan.weeklies.data.shopping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CombineShoppingLineTest {

    @Test
    fun sumsSameLine() {
        assertEquals("200g pasta", combineShoppingLine("100g pasta", "100g pasta"))
    }

    @Test
    fun matchesAttachedAndSpacedAmounts() {
        assertEquals("200 g pasta", combineShoppingLine("100 g pasta", "100g pasta"))
    }

    @Test
    fun sumsCountablesWithoutUnit() {
        assertEquals("5 onions", combineShoppingLine("2 onions", "3 onions"))
    }

    @Test
    fun sumsFractions() {
        assertEquals("0.75 cup milk", combineShoppingLine("1/2 cup milk", "1/4 cup milk"))
    }

    @Test
    fun sumsDecimals() {
        assertEquals("2 cups flour", combineShoppingLine("1.5 cups flour", "0.5 cups flour"))
    }

    @Test
    fun mergesNumberMidLine() {
        assertEquals("bag of rice 1000g", combineShoppingLine("bag of rice 500g", "bag of rice 500g"))
    }

    @Test
    fun mergesOnlyFirstNumberOfCompoundLines() {
        assertEquals(
            "4 28-oz cans tomatoes",
            combineShoppingLine("2 28-oz cans tomatoes", "2 28-oz cans tomatoes"),
        )
    }

    @Test
    fun keepsExistingLineSpacing() {
        assertEquals("200 g  pasta", combineShoppingLine("100 g  pasta", "100g pasta"))
    }

    @Test
    fun sumsAmountsIntoExistingLineSpelling() {
        assertEquals("300 g pasta", combineShoppingLine("100 g pasta", "200g pasta"))
    }

    @Test
    fun returnsNullForDifferentNames() {
        assertNull(combineShoppingLine("100g pasta", "100g rice"))
    }

    @Test
    fun returnsNullForDifferentUnits() {
        assertNull(combineShoppingLine("100 g pasta", "100 ml pasta"))
    }

    @Test
    fun returnsNullForUnitSpellingDifferences() {
        assertNull(combineShoppingLine("100 grams pasta", "100g pasta"))
    }

    @Test
    fun returnsNullForPluralDifferences() {
        assertNull(combineShoppingLine("2 eggs", "1 egg"))
    }

    @Test
    fun returnsNullForCaseDifferences() {
        assertNull(combineShoppingLine("100 g Pasta", "100 g pasta"))
    }

    @Test
    fun returnsNullWhenEitherLineHasNoNumber() {
        assertNull(combineShoppingLine("pasta", "100g pasta"))
        assertNull(combineShoppingLine("100g pasta", "pasta"))
    }

    @Test
    fun returnsNullForProseLines() {
        assertNull(combineShoppingLine("salt to taste", "1 pinch salt"))
    }

    @Test
    fun returnsNullForEmptyLines() {
        assertNull(combineShoppingLine("", "100g pasta"))
        assertNull(combineShoppingLine("100g pasta", ""))
    }
}
