package io.github.conorryan.weeklies.ui.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.conorryan.weeklies.R
import io.github.conorryan.weeklies.data.model.Recipe
import io.github.conorryan.weeklies.ui.Eyebrow

/** Largest portion count the stepper offers. */
const val MAX_PORTIONS_STEPPER = 99

/**
 * Asks how many portions of [recipe] to add to the shopping list, starting at
 * [defaultPortions] (the Settings value), with a − / + stepper.
 */
@Composable
fun PortionsDialog(
    recipe: Recipe,
    defaultPortions: Int,
    onAdd: (portions: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var portions by rememberSaveable(recipe.id) { mutableIntStateOf(defaultPortions.coerceIn(1, MAX_PORTIONS_STEPPER)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Eyebrow(stringResource(R.string.portions_dialog_eyebrow))
                Text(recipe.name, style = MaterialTheme.typography.headlineSmall)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.shopping_portions_label), style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.medium)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilledIconButton(
                            onClick = { portions-- },
                            enabled = portions > 1,
                            shape = MaterialTheme.shapes.small,
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            modifier = Modifier.size(52.dp),
                        ) {
                            Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.portions_fewer))
                        }
                        Text(
                            portions.toString(),
                            style = MaterialTheme.typography.displaySmall,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        )
                        FilledIconButton(
                            onClick = { portions++ },
                            enabled = portions < MAX_PORTIONS_STEPPER,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.size(52.dp),
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.portions_more))
                        }
                    }
                    Text(
                        if (portions == defaultPortions) {
                            stringResource(R.string.portions_default_hint)
                        } else {
                            stringResource(R.string.portions_default_other, defaultPortions)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.small)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        pluralStringResource(R.plurals.portions_lines_added, recipe.ingredients.size, recipe.ingredients.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        stringResource(R.string.portions_merge_note),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(portions) }) {
                Text(pluralStringResource(R.plurals.portions_add_button, portions, portions))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
