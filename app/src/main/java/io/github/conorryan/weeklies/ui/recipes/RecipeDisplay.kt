package io.github.conorryan.weeklies.ui.recipes

import io.github.conorryan.weeklies.data.model.DayAssignments
import io.github.conorryan.weeklies.data.model.Weekday
import io.github.conorryan.weeklies.data.quantity.formatScaled

/**
 * Splits an instructions blob into steps for a numbered list: one step per
 * non-blank line, with any list marker the user typed ("- ", "2.", "3)")
 * removed. A single paragraph stays one step.
 */
fun instructionSteps(instructions: String): List<String> =
    instructions.lines()
        .map { it.trim().replace(Regex("""^(?:[-*•]\s*|\d+\s*[.)]\s*)"""), "").trim() }
        .filter { it.isNotEmpty() }

/** The weekdays that have [recipeId] assigned, in week order. */
fun plannedDays(byDay: DayAssignments, recipeId: String): List<Weekday> =
    Weekday.entries.filter { byDay[it] == recipeId }

/** One quick scale choice: ÷6, ÷4, ÷2, ×2, ×4, ×6. */
data class ScalePreset(val value: Int, val divide: Boolean) {
    val factor: Double get() = if (divide) 1.0 / value else value.toDouble()
    val label: String get() = scaleLabel(value.toDouble(), divide)
}

val SCALE_PRESETS = listOf(
    ScalePreset(6, divide = true),
    ScalePreset(4, divide = true),
    ScalePreset(2, divide = true),
    ScalePreset(2, divide = false),
    ScalePreset(4, divide = false),
    ScalePreset(6, divide = false),
)

/** "×1.5", "÷4". */
fun scaleLabel(value: Double, divide: Boolean): String = (if (divide) "÷" else "×") + formatScaled(value)
