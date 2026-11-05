package io.github.conorryan.weeklies.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.conorryan.weeklies.R
import io.github.conorryan.weeklies.data.model.DayAssignments
import io.github.conorryan.weeklies.data.model.Weekday
import io.github.conorryan.weeklies.data.quantity.formatScaled
import io.github.conorryan.weeklies.ui.CardDivider

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

/**
 * One row of the recipe page's ingredients card: the saved per-portion
 * calories, view-only. Recipes with no estimate show nothing; they are only
 * ever set in the edit form.
 */
@Composable
fun CaloriesRow(calories: Int) {
    CardDivider()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Outlined.LocalFireDepartment,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            stringResource(R.string.calories_row_caption),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            stringResource(R.string.calories_kcal_value, calories),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}
