package io.github.conorryan.weeklies.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.conorryan.weeklies.R
import io.github.conorryan.weeklies.data.import.isUnchangedPairing
import io.github.conorryan.weeklies.data.import.pairCleanupForReview
import io.github.conorryan.weeklies.data.model.NewRecipe
import io.github.conorryan.weeklies.ui.ConfirmDialog
import io.github.conorryan.weeklies.ui.InfoDialog

/** Bold label above a form field. */
@Composable
private fun FormLabel(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
}

/**
 * Shared recipe entry form for new, edit, and import-review flows: name,
 * editable ingredient rows, instructions, validation, and the AI cleanup
 * previews. Pure rendering: all logic lives in [RecipeFormViewModel].
 */
@Composable
fun RecipeForm(
    state: RecipeFormUiState,
    vm: RecipeFormViewModel,
    submitLabel: String = stringResource(R.string.recipe_save),
    onSubmit: (NewRecipe) -> Unit,
) {
    val draft = state.draft
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FormLabel(stringResource(R.string.recipe_name_label))
        OutlinedTextField(
            value = draft.name,
            onValueChange = vm::setName,
            placeholder = { Text(stringResource(R.string.recipe_name_hint)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        FormLabel(stringResource(R.string.recipe_ingredients_label))
        for (index in draft.ingredients.indices) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft.ingredients[index],
                    onValueChange = { vm.setIngredient(index, it) },
                    placeholder = { Text(stringResource(R.string.recipe_ingredient_hint)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { vm.removeIngredient(index) },
                    enabled = draft.ingredients.size > 1,
                ) {
                    Icon(
                        Icons.Filled.Remove,
                        contentDescription = stringResource(R.string.recipe_remove_ingredient),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = vm::addIngredient) {
                Text(stringResource(R.string.recipe_add_ingredient), fontWeight = FontWeight.SemiBold)
            }
            TextButton(
                onClick = vm::cleanIngredients,
                enabled = state.cleanupBusy == null && state.hasIngredientText,
            ) {
                Text(
                    if (state.cleanupBusy == CleanupTarget.INGREDIENTS) {
                        stringResource(R.string.recipe_cleaning_up)
                    } else {
                        stringResource(R.string.recipe_clean_ingredients)
                    },
                )
            }
        }

        FormLabel(stringResource(R.string.recipe_instructions_label))
        OutlinedTextField(
            value = draft.instructions,
            onValueChange = vm::setInstructions,
            placeholder = { Text(stringResource(R.string.recipe_instructions_hint)) },
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(
            onClick = vm::cleanInstructions,
            enabled = state.cleanupBusy == null && state.hasInstructions,
        ) {
            Text(
                if (state.cleanupBusy == CleanupTarget.INSTRUCTIONS) {
                    stringResource(R.string.recipe_cleaning_up)
                } else {
                    stringResource(R.string.recipe_clean_instructions)
                },
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { vm.submit(onSubmit) }) {
                Text(submitLabel, fontWeight = FontWeight.SemiBold)
            }
        }

        val validationError = state.validationError
        if (validationError != null) {
            Text(stringResource(validationError), color = MaterialTheme.colorScheme.error)
        }

        val preview = state.ingredientPreview
        if (preview != null) {
            val pairings = pairCleanupForReview(draft.ingredients.map { it.trim() }.filter { it.isNotEmpty() }, preview)
            val hasChanges = pairings.any { !isUnchangedPairing(it) }
            ConfirmDialog(
                title = if (hasChanges) {
                    stringResource(R.string.recipe_apply_cleanup)
                } else {
                    stringResource(R.string.recipe_nothing_to_clean)
                },
                confirmLabel = stringResource(if (hasChanges) R.string.apply else R.string.close),
                dismissLabel = if (hasChanges) stringResource(R.string.cancel) else null,
                onConfirm = {
                    if (hasChanges) vm.applyIngredientCleanup() else vm.dismissIngredientCleanup()
                },
                onDismiss = vm::dismissIngredientCleanup,
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        if (!hasChanges) {
                            Text(stringResource(R.string.recipe_ingredients_look_good))
                        }
                        val unchanged = pairings.count { isUnchangedPairing(it) }
                        if (unchanged > 0) {
                            Text(
                                pluralStringResource(
                                    R.plurals.recipe_unchanged_count,
                                    unchanged,
                                    unchanged,
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        for (pair in pairings.filterNot { isUnchangedPairing(it) }) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                for (line in pair.before) {
                                    Text(
                                        line,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                for (line in pair.after) {
                                    Text("→ $line")
                                }
                                if (pair.before.size > 1 && pair.after.size == 1) {
                                    Text(
                                        stringResource(R.string.recipe_merged),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else if (pair.after.size > 1) {
                                    Text(
                                        stringResource(R.string.recipe_split),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                },
            )
        }

        val newInstructions = state.instructionPreview
        if (newInstructions != null) {
            val hasChanges = newInstructions != draft.instructions.trim()
            ConfirmDialog(
                title = if (hasChanges) {
                    stringResource(R.string.recipe_apply_cleanup)
                } else {
                    stringResource(R.string.recipe_nothing_to_clean)
                },
                confirmLabel = stringResource(if (hasChanges) R.string.apply else R.string.close),
                dismissLabel = if (hasChanges) stringResource(R.string.cancel) else null,
                onConfirm = {
                    if (hasChanges) vm.applyInstructionCleanup() else vm.dismissInstructionCleanup()
                },
                onDismiss = vm::dismissInstructionCleanup,
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        if (!hasChanges) {
                            Text(stringResource(R.string.recipe_instructions_look_good))
                        }
                        for (line in newInstructions.lines()) {
                            Text(line)
                        }
                    }
                },
            )
        }

        val cleanupErrorDisplay = state.cleanupError
        if (cleanupErrorDisplay != null) {
            InfoDialog(
                title = stringResource(cleanupErrorDisplay.title),
                onDismiss = vm::dismissCleanupError,
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (cleanupErrorDisplay.messageArg != null) {
                                stringResource(cleanupErrorDisplay.message, cleanupErrorDisplay.messageArg)
                            } else {
                                stringResource(cleanupErrorDisplay.message)
                            },
                        )
                        if (cleanupErrorDisplay.hint != null) {
                            Text(stringResource(cleanupErrorDisplay.hint))
                        }
                    }
                },
            )
        }
    }
}
