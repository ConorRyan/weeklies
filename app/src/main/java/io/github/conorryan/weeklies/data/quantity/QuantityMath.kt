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

/** Multiplies every numeric token in each of [lines] by [factor]. */
fun scaleIngredientLines(lines: List<String>, factor: Double): List<String> =
    lines.map { scaleIngredientLine(it, factor) }

/** Unit words that belong with a leading amount when splitting a line for display. */
private val DISPLAY_UNITS = setOf(
    "g", "kg", "mg", "ml", "cl", "dl", "l",
    "tsp", "tbsp", "cup", "cups", "oz", "lb", "lbs",
    "c.", "c", "càs", "càc",
)

private val LEADING_AMOUNT = Regex("""^\s*(${NUMERIC_TOKEN.pattern})(?:\s*([A-Za-zàéè.]+))?\s+(.+)$""")

/**
 * Splits an ingredient line into its leading amount (with a following unit
 * word when it is a known unit) and the rest, for a two-column display:
 * "75 g arborio rice" -> ("75 g", "arborio rice"). Null when the line does
 * not start with a number.
 */
fun splitLeadingQuantity(line: String): Pair<String, String>? {
    val match = LEADING_AMOUNT.find(line) ?: return null
    val amount = match.groupValues[1].trim()
    val unit = match.groupValues[2]
    val rest = match.groupValues[3].trim()
    return if (unit.isNotEmpty() && unit.lowercase() in DISPLAY_UNITS) {
        "$amount $unit" to rest
    } else if (unit.isNotEmpty()) {
        amount to "$unit $rest"
    } else {
        amount to rest
    }
}

/** Largest multiply or divide value the scale picker accepts. */
const val MAX_SCALE_VALUE = 100.0

/**
 * Parses a user-typed scale value ("1.5", "0,75") into a factor; [divide]
 * inverts it. Null for blank, non-numeric, zero, negative, or > [MAX_SCALE_VALUE].
 */
fun parseScaleFactor(text: String, divide: Boolean): Double? {
    val value = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
    if (value.isNaN() || value.isInfinite() || value <= 0.0 || value > MAX_SCALE_VALUE) return null
    return if (divide) 1.0 / value else value
}
