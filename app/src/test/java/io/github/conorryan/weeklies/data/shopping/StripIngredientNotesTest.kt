package io.github.conorryan.weeklies.data.shopping

import org.junit.Assert.assertEquals
import org.junit.Test

class StripIngredientNotesTest {

    @Test
    fun removesTrailingParenthesizedNote() {
        assertEquals("250 g mushrooms", stripIngredientNotes("250 g mushrooms (trimmed)"))
        assertEquals("1 onion", stripIngredientNotes("1 onion (finely chopped)"))
    }

    @Test
    fun removesMultipleTrailingNotes() {
        assertEquals("250 g mushrooms", stripIngredientNotes("250 g mushrooms (trimmed) (halved)"))
    }

    @Test
    fun leavesLinesWithoutNotesAlone() {
        assertEquals("250 g mushrooms", stripIngredientNotes("250 g mushrooms"))
        assertEquals("2 tbsp olive oil", stripIngredientNotes(" 2 tbsp olive oil "))
    }

    @Test
    fun noteOnlyLineBecomesEmpty() {
        assertEquals("pepper", stripIngredientNotes("pepper (to taste)"))
        assertEquals("", stripIngredientNotes("(to taste)"))
    }

    @Test
    fun strippedLinesMergeWithPlainLines() {
        val plain = "250 g mushrooms"
        val withNote = stripIngredientNotes("250 g mushrooms (trimmed)")
        assertEquals("500 g mushrooms", combineShoppingLine(plain, withNote))
    }

    @Test
    fun unstrippedNotesBlockMerging() {
        // Sanity check for the behavior the strip fixes.
        assertEquals(null, combineShoppingLine("250 g mushrooms", "250 g mushrooms (trimmed)"))
    }
}
