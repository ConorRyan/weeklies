package com.conorryan.weeklies.data.shopping

import com.conorryan.weeklies.data.import.NUMERIC_TOKEN
import com.conorryan.weeklies.data.import.formatScaled
import com.conorryan.weeklies.data.import.parseNumericToken

private data class SplitLine(val before: String, val amount: Double, val after: String)

private fun splitOnFirstNumber(line: String): SplitLine? {
    val match = NUMERIC_TOKEN.find(line) ?: return null
    val amount = parseNumericToken(match.value) ?: return null
    return SplitLine(
        before = line.substring(0, match.range.first),
        amount = amount,
        after = line.substring(match.range.last + 1),
    )
}

/** The line without its first number, for comparison. */
private fun restKey(line: SplitLine): String =
    (line.before + line.after).replace(Regex("""\s+"""), " ").trim()

/**
 * Sums the first number found in two shopping lines when the rest of
 * each line is the same (ignoring whitespace), e.g. "100g pasta" +
 * "100 g pasta" = "200g pasta". The sum is written back inside the
 * existing line, so its formatting is preserved. Returns null when
 * either line has no number or the rest differs.
 */
fun combineShoppingLine(existing: String, incoming: String): String? {
    val a = splitOnFirstNumber(existing) ?: return null
    val b = splitOnFirstNumber(incoming) ?: return null
    if (restKey(a) != restKey(b)) return null
    return a.before + formatScaled(a.amount + b.amount) + a.after
}
