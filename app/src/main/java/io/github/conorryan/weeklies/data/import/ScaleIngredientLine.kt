package io.github.conorryan.weeklies.data.import

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
