package io.github.conorryan.weeklies.data.quantity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Line splitting for the two-column ingredient display, user-typed scale factors, and list scaling. */
class QuantityDisplayTest {

    @Test
    fun splitsAmountAndKnownUnit() {
        assertEquals("75 g" to "arborio rice", splitLeadingQuantity("75 g arborio rice"))
    }

    @Test
    fun splitsUnitWrittenAgainstTheNumber() {
        assertEquals("60 g" to "chestnut mushrooms, sliced", splitLeadingQuantity("60g chestnut mushrooms, sliced"))
    }

    @Test
    fun keepsNonUnitWordWithTheIngredient() {
        assertEquals("1" to "whole chicken", splitLeadingQuantity("1 whole chicken"))
        assertEquals("2" to "lemons", splitLeadingQuantity("2 lemons"))
    }

    @Test
    fun splitsFractionsAndDecimals() {
        assertEquals("1/4" to "onion", splitLeadingQuantity("1/4 onion"))
        assertEquals("1.5 l" to "stock", splitLeadingQuantity("1.5 l stock"))
    }

    @Test
    fun lineWithoutLeadingNumberIsNotSplit() {
        assertNull(splitLeadingQuantity("salt and pepper"))
        assertNull(splitLeadingQuantity("12"))
    }

    @Test
    fun parsesPointAndCommaDecimals() {
        assertEquals(1.5, parseScaleFactor("1.5", divide = false)!!, 1e-9)
        assertEquals(0.75, parseScaleFactor(" 0,75 ", divide = false)!!, 1e-9)
    }

    @Test
    fun divideInvertsTheValue() {
        assertEquals(0.25, parseScaleFactor("4", divide = true)!!, 1e-9)
    }

    @Test
    fun rejectsUnusableValues() {
        assertNull(parseScaleFactor("", divide = false))
        assertNull(parseScaleFactor("abc", divide = false))
        assertNull(parseScaleFactor("0", divide = false))
        assertNull(parseScaleFactor("-2", divide = true))
        assertNull(parseScaleFactor("101", divide = false))
    }

    @Test
    fun acceptsTheMaximum() {
        assertEquals(100.0, parseScaleFactor("100", divide = false)!!, 1e-9)
    }

    @Test
    fun scalesEveryLine() {
        assertEquals(
            listOf("150 g rice", "salt", "0.5 onion"),
            scaleIngredientLines(listOf("75 g rice", "salt", "1/4 onion"), 2.0),
        )
    }
}
