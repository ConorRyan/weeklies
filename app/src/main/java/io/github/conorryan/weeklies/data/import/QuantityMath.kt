package io.github.conorryan.weeklies.data.import

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Matches mixed numbers (`1 1/2`), fractions (`1/2`, `1 / 2`),
 * comma-grouped integers (`1,000`), decimals, and plain integers.
 */
internal val NUMERIC_TOKEN = Regex(
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

internal fun parseNumericToken(raw: String): Double? {
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

internal fun formatScaled(n: Double): String {
    if (n.isNaN() || n.isInfinite()) return n.toString()
    val rounded = (n * 10000).roundToLong() / 10000.0
    if (abs(rounded - rounded.roundToLong().toDouble()) < 1e-9) {
        return rounded.roundToLong().toString()
    }
    return rounded.toString()
}
