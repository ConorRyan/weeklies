package io.github.conorryan.weeklies.data.shopping

private val TRAILING_NOTE = Regex("""\s*\([^()]*\)\s*$""")

/**
 * Removes preparation notes in parentheses from the end of an ingredient line:
 * "250 g mushrooms (trimmed)" becomes "250 g mushrooms", "pepper (to taste)"
 * becomes "pepper", and a line that is only a note ("(to taste)") comes back
 * empty. The shopping list merges on the ingredient itself, so notes like
 * "(finely chopped)" would otherwise keep identical ingredients from combining.
 */
fun stripIngredientNotes(line: String): String {
    var result = line.trim()
    while (true) {
        val next = result.replace(TRAILING_NOTE, "")
        if (next == result) return result.trim()
        result = next
    }
}
