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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    submitLabel: String = "Save recipe",
    onSubmit: (NewRecipe) -> Unit,
) {
    val draft = state.draft
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FormLabel("Name")
        OutlinedTextField(
            value = draft.name,
            onValueChange = vm::setName,
            placeholder = { Text("Recipe name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        FormLabel("Ingredients")
        for (index in draft.ingredients.indices) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft.ingredients[index],
                    onValueChange = { vm.setIngredient(index, it) },
                    placeholder = { Text("Ingredient") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { vm.removeIngredient(index) },
                    enabled = draft.ingredients.size > 1,
                ) {
                    Icon(
                        Icons.Filled.Remove,
                        contentDescription = "Remove ingredient",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = vm::addIngredient) {
                Text("Add ingredient", fontWeight = FontWeight.SemiBold)
            }
            TextButton(
                onClick = vm::cleanIngredients,
                enabled = state.cleanupBusy == null && state.hasIngredientText,
            ) {
                Text(
                    if (state.cleanupBusy == CleanupTarget.INGREDIENTS) "Cleaning up…" else "Clean up ingredients ✨",
                )
            }
        }

        FormLabel("Instructions")
        OutlinedTextField(
            value = draft.instructions,
            onValueChange = vm::setInstructions,
            placeholder = { Text("Steps, notes, timing…") },
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(
            onClick = vm::cleanInstructions,
            enabled = state.cleanupBusy == null && state.hasInstructions,
        ) {
            Text(
                if (state.cleanupBusy == CleanupTarget.INSTRUCTIONS) "Cleaning up…" else "Clean up instructions ✨",
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { vm.submit(onSubmit) }) {
                Text(submitLabel, fontWeight = FontWeight.SemiBold)
            }
        }

        val validationError = state.validationError
        if (validationError != null) {
            Text(validationError, color = MaterialTheme.colorScheme.error)
        }

        val preview = state.ingredientPreview
        if (preview != null) {
            val pairings = pairCleanupForReview(draft.ingredients.map { it.trim() }.filter { it.isNotEmpty() }, preview)
            val hasChanges = pairings.any { !isUnchangedPairing(it) }
            ConfirmDialog(
                title = if (hasChanges) "Apply cleanup?" else "Nothing to clean up",
                confirmLabel = if (hasChanges) "Apply" else "Close",
                dismissLabel = if (hasChanges) "Cancel" else null,
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
                            Text("Your ingredients already look good - no changes suggested.")
                        }
                        val unchanged = pairings.count { isUnchangedPairing(it) }
                        if (unchanged > 0) {
                            Text(
                                "$unchanged ingredient${if (unchanged == 1) "" else "s"} unchanged",
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
                                        "merged",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                } else if (pair.after.size > 1) {
                                    Text(
                                        "split",
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
                title = if (hasChanges) "Apply cleanup?" else "Nothing to clean up",
                confirmLabel = if (hasChanges) "Apply" else "Close",
                dismissLabel = if (hasChanges) "Cancel" else null,
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
                            Text("Your instructions already look good - no changes suggested.")
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
                title = cleanupErrorDisplay.title,
                onDismiss = vm::dismissCleanupError,
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(cleanupErrorDisplay.message)
                        if (cleanupErrorDisplay.hint != null) {
                            Text(cleanupErrorDisplay.hint)
                        }
                    }
                },
            )
        }
    }
}
