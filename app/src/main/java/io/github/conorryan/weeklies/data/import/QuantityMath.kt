package io.github.conorryan.weeklies.data.import

import kotlin.math.abs
import kotlin.math.roundToLong

/** Matches integers, decimals, and fractions like `1/2` or `1 / 2`. */
internal val NUMERIC_TOKEN = Regex("""\d+\s*/\s*\d+|\d+\.\d+|\d+""")

internal fun parseNumericToken(raw: String): Double? {
    val t = raw.trim()
    if (t.contains('/')) {
        val parts = t.split(Regex("""\s*/\s*"""))
        if (parts.size != 2) return null
        val a = parts[0].toDoubleOrNull() ?: return null
        val b = parts[1].toDoubleOrNull() ?: return null
        if (b == 0.0) return null
        return a / b
    }
    return t.toDoubleOrNull()?.takeIf { it.isFinite() }
}

internal fun formatScaled(n: Double): String {
    if (n.isNaN() || n.isInfinite()) return n.toString()
    val rounded = (n * 10000).roundToLong() / 10000.0
    if (abs(rounded - rounded.roundToLong().toDouble()) < 1e-9) {
        return rounded.roundToLong().toString()
    }
    return rounded.toString()
}
