package io.github.conorryan.weeklies.data.quantity

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Ingredient-quantity math shared by the recipe import scaler and the
 * shopping-list merge: token matching, parsing, formatting, and line scaling
 * live here so neither feature reaches into the other's machinery.
 */

/**
 * Matches mixed numbers (`1 1/2`), fractions (`1/2`, `1 / 2`),
 * comma-grouped integers (`1,000`), decimals, and plain integers.
 */
val NUMERIC_TOKEN = Regex(
    """\d+\s+\d+\s*/\s*\d+|\d+\s*/\s*\d+|\d{1,3}(?:,\d{3})+|\d+\.\d+|\d+""",
)

private fun parseFraction(token: String): Double? {
    val parts = token.split(Regex("""\s*/\s*"""))
    if (parts.size != 2) return null
    val a = parts[0].toDoubleOrNull() ?: return null
    val b = parts[1].toDoubleOrNull() ?: return null
    if (b == 0.0) return null
    return a / b
}

fun parseNumericToken(raw: String): Double? {
    val t = raw.trim().replace(",", "").replace(Regex("""\s*/\s*"""), "/")
    val parts = t.split(Regex("""\s+"""))
    if (parts.size == 2 && parts[1].contains("/")) {
        val whole = parts[0].toDoubleOrNull() ?: return null
        val fraction = parseFraction(parts[1]) ?: return null
        return whole + fraction
    }
    if (parts.size == 1) {
        val token = parts[0]
        if (token.contains("/")) return parseFraction(token)
        return token.toDoubleOrNull()?.takeIf { it.isFinite() }
    }
    return null
}

fun formatScaled(n: Double): String {
    if (n.isNaN() || n.isInfinite()) return n.toString()
    val rounded = (n * 10000).roundToLong() / 10000.0
    if (abs(rounded - rounded.roundToLong().toDouble()) < 1e-9) {
        return rounded.roundToLong().toString()
    }
    return rounded.toString()
}

/**
 * Multiplies every numeric token in an ingredient line by [factor].
 * Handles integers, decimals, and fractions like `1/2` or `1 / 2`.
 */
fun scaleIngredientLine(line: String, factor: Double): String {
    if (factor.isNaN() || factor.isInfinite() || factor == 1.0) return line
    return NUMERIC_TOKEN.replace(line) { match ->
        val value = parseNumericToken(match.value) ?: return@replace match.value
        formatScaled(value * factor)
    }
}

/**
 * Joins an optional quantity and an ingredient name into one line; null when
 * nothing usable remains (empty name).
 */
fun ingredientPartsToLine(quantity: String?, name: String): String? {
    val n = name.trim()
    if (n.isEmpty()) return null
    val q = quantity?.trim() ?: ""
    val line = listOf(q, n).filter { it.isNotEmpty() }.joinToString(" ")
    return line.ifEmpty { null }
}
