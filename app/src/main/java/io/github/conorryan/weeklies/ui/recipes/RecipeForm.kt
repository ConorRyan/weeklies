package io.github.conorryan.weeklies.ui.recipes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import io.github.conorryan.weeklies.R
import io.github.conorryan.weeklies.data.import.CalorieConfidence
import io.github.conorryan.weeklies.data.import.CleanupPairing
import io.github.conorryan.weeklies.data.model.NewRecipe
import io.github.conorryan.weeklies.ui.CardDivider
import io.github.conorryan.weeklies.ui.CardGroup
import io.github.conorryan.weeklies.ui.Eyebrow
import io.github.conorryan.weeklies.ui.InfoDialog

/** Section label with an optional "Tidy with AI" action at the end. */
@Composable
private fun FormSection(
    label: String,
    aiBusy: Boolean = false,
    onAi: (() -> Unit)? = null,
    aiEnabled: Boolean = true,
    aiLabel: String = stringResource(R.string.recipe_tidy_with_ai),
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Eyebrow(label, Modifier.weight(1f))
        if (onAi != null) AiChip(busy = aiBusy, enabled = aiEnabled, label = aiLabel, onClick = onAi)
    }
}

@Composable
private fun AiChip(busy: Boolean, enabled: Boolean, label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled && !busy,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.heightIn(min = 36.dp),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onTertiaryContainer)
            } else {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
            }
            Text(
                if (busy) stringResource(R.string.recipe_cleaning_up) else label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun formFieldColors() = OutlinedTextFieldDefaults.colors(
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
)

/**
 * Shared recipe entry form for new, edit, and import-review flows: name,
 * editable ingredient rows, instructions, validation, and the AI cleanup
 * reviews. Pure rendering: all logic lives in [RecipeFormViewModel]. The
 * submit button lives in the screen's top bar, which calls [RecipeFormViewModel.submit].
 */
@Composable
fun RecipeForm(
    state: RecipeFormUiState,
    vm: RecipeFormViewModel,
) {
    val draft = state.draft
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        val validationError = state.validationError
        if (validationError != null) {
            Text(
                stringResource(validationError),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer, MaterialTheme.shapes.small)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            FormSection(stringResource(R.string.recipe_name_label))
            OutlinedTextField(
                value = draft.name,
                onValueChange = vm::setName,
                placeholder = { Text(stringResource(R.string.recipe_name_hint)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge,
                shape = MaterialTheme.shapes.small,
                colors = formFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FormSection(
                label = stringResource(R.string.recipe_ingredients_one_portion),
                aiBusy = state.cleanupBusy == CleanupTarget.INGREDIENTS,
                aiEnabled = state.cleanupBusy == null && state.hasIngredientText,
                onAi = vm::cleanIngredients,
            )
            CardGroup {
                draft.ingredients.forEachIndexed { index, line ->
                    if (index > 0) CardDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = line,
                            onValueChange = { vm.setIngredient(index, it) },
                            placeholder = { Text(stringResource(R.string.recipe_ingredient_hint)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                unfocusedBorderColor = Color.Transparent,
                                focusedBorderColor = Color.Transparent,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { vm.removeIngredient(index) },
                            enabled = draft.ingredients.size > 1,
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.recipe_remove_ingredient),
                                tint = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                }
                CardDivider()
                TextButton(
                    onClick = vm::addIngredient,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 4.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        stringResource(R.string.recipe_add_ingredient),
                        modifier = Modifier.weight(1f).padding(start = 10.dp),
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FormSection(
                label = stringResource(R.string.calories_row_caption),
                aiBusy = state.cleanupBusy == CleanupTarget.CALORIES,
                aiEnabled = state.cleanupBusy == null && state.hasIngredientText,
                aiLabel = stringResource(R.string.calories_estimate_chip),
                onAi = vm::estimateCalories,
            )
            OutlinedTextField(
                value = draft.caloriesText,
                onValueChange = vm::setCalories,
                placeholder = { Text(stringResource(R.string.calories_field_label)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge,
                shape = MaterialTheme.shapes.small,
                colors = formFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            // The offered guess with its verdict; typing anything else dismisses it.
            val estimate = state.calorieEstimate
            if (estimate != null) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(14.dp),
                        )
                        Column(
                            Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    stringResource(R.string.recipe_suggested_by_mistral),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                )
                                Text(
                                    stringResource(estimate.confidence.label),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.extraLarge)
                                        .padding(horizontal = 10.dp, vertical = 3.dp),
                                )
                            }
                            if (estimate.rationale.isNotEmpty()) {
                                Text(
                                    estimate.rationale,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(onClick = vm::dismissCalorieEstimate) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = stringResource(R.string.close),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FormSection(
                label = stringResource(R.string.recipe_instructions_label),
                aiBusy = state.cleanupBusy == CleanupTarget.INSTRUCTIONS,
                // With no instructions yet, the chip asks Mistral to write the
                // steps from the recipe name and ingredients instead of tidying.
                aiEnabled = state.cleanupBusy == null &&
                    (state.hasInstructions || state.hasIngredientText),
                aiLabel = stringResource(
                    if (state.hasInstructions) R.string.recipe_tidy_with_ai else R.string.recipe_write_with_ai,
                ),
                onAi = vm::cleanInstructions,
            )
            OutlinedTextField(
                value = draft.instructions,
                onValueChange = vm::setInstructions,
                placeholder = { Text(stringResource(R.string.recipe_instructions_hint)) },
                minLines = 5,
                shape = MaterialTheme.shapes.medium,
                colors = formFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (state.ingredientPreview != null) {
        IngredientCleanupSheet(state, vm)
    }

    val newInstructions = state.instructionPreview
    if (newInstructions != null) {
        InstructionCleanupSheet(
            before = draft.instructions.trim(),
            after = newInstructions,
            onApply = vm::applyInstructionCleanup,
            onDismiss = vm::dismissInstructionCleanup,
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

/** Header shared by both cleanup sheets: "Suggested by Mistral" over a title. */
@Composable
private fun CleanupHeader(title: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(14.dp),
            )
            Eyebrow(stringResource(R.string.recipe_suggested_by_mistral), color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
        Text(title, style = MaterialTheme.typography.headlineSmall)
    }
}

/**
 * Lists each ingredient change the cleanup suggests with its own Keep mine /
 * Use AI switch; Apply uses only the accepted ones.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientCleanupSheet(state: RecipeFormUiState, vm: RecipeFormViewModel) {
    val changed = state.changedPairings
    val unchanged = state.ingredientPairings.size - changed.size
    val acceptedCount = changed.count { it in state.acceptedPairings }
    ModalBottomSheet(
        onDismissRequest = vm::dismissIngredientCleanup,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Column(
                Modifier.padding(horizontal = 20.dp).padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CleanupHeader(
                    stringResource(if (changed.isEmpty()) R.string.recipe_nothing_to_clean else R.string.recipe_review_changes),
                )
                if (changed.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.recipe_changes_accepted, acceptedCount, changed.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = vm::acceptAllPairings) { Text(stringResource(R.string.recipe_accept_all)) }
                        TextButton(
                            onClick = vm::rejectAllPairings,
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurfaceVariant),
                        ) { Text(stringResource(R.string.recipe_reject_all)) }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (changed.isEmpty()) {
                    Text(stringResource(R.string.recipe_ingredients_look_good))
                }
                for (index in changed) {
                    PairingCard(
                        pairing = state.ingredientPairings[index],
                        accepted = index in state.acceptedPairings,
                        onAccept = { vm.setPairingAccepted(index, it) },
                    )
                }
                if (unchanged > 0) {
                    Text(
                        pluralStringResource(R.plurals.recipe_unchanged_count, unchanged, unchanged),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (changed.isEmpty()) {
                    Button(onClick = vm::dismissIngredientCleanup, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                        Text(stringResource(R.string.close))
                    }
                } else {
                    OutlinedButton(
                        onClick = vm::dismissIngredientCleanup,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    ) { Text(stringResource(R.string.cancel)) }
                    Button(
                        onClick = vm::applyIngredientCleanup,
                        modifier = Modifier.weight(1.4f).heightIn(min = 52.dp),
                    ) {
                        Text(
                            if (acceptedCount == 0) {
                                stringResource(R.string.recipe_keep_my_version)
                            } else {
                                pluralStringResource(R.plurals.recipe_apply_changes, acceptedCount, acceptedCount)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PairingCard(
    pairing: CleanupPairing,
    accepted: Boolean,
    onAccept: (Boolean) -> Unit,
) {
    val kind = when {
        pairing.before.size > 1 && pairing.after.size == 1 -> R.string.recipe_merged
        pairing.after.size > 1 -> R.string.recipe_split
        pairing.after.isEmpty() -> R.string.recipe_removed_line
        pairing.before.isEmpty() -> R.string.recipe_added_line
        else -> R.string.recipe_reworded
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (accepted) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (accepted) 1.5.dp else 1.dp,
            if (accepted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(start = 2.dp)) {
                Text(
                    stringResource(kind),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.extraLarge)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
                for (line in pairing.before) {
                    Text(
                        line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (accepted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (accepted) TextDecoration.LineThrough else null,
                    )
                }
                for (line in pairing.after) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            line,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (accepted) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = if (accepted) null else TextDecoration.LineThrough,
                        )
                    }
                }
            }
            KeepOrUse(accepted = accepted, onAccept = onAccept)
        }
    }
}

/** Two-option segmented switch: "Keep mine" / "Use AI". */
@Composable
private fun KeepOrUse(accepted: Boolean, onAccept: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background, MaterialTheme.shapes.small)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        SegmentOption(
            label = stringResource(R.string.recipe_keep_mine),
            icon = Icons.Filled.Close,
            selected = !accepted,
            selectedColor = MaterialTheme.colorScheme.surface,
            selectedContent = MaterialTheme.colorScheme.onSurface,
            onClick = { onAccept(false) },
            modifier = Modifier.weight(1f),
        )
        SegmentOption(
            label = stringResource(R.string.recipe_use_ai),
            icon = Icons.Filled.Check,
            selected = accepted,
            selectedColor = MaterialTheme.colorScheme.primary,
            selectedContent = MaterialTheme.colorScheme.onPrimary,
            onClick = { onAccept(true) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SegmentOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    selectedColor: Color,
    selectedContent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .heightIn(min = 44.dp)
            .background(if (selected) selectedColor else Color.Transparent, MaterialTheme.shapes.extraSmall)
            .clickable(role = Role.RadioButton, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tint = if (selected) selectedContent else MaterialTheme.colorScheme.onSurfaceVariant
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = tint,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}

/** Instructions are one text blob, so their review is a single before/after with Keep mine or Apply. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InstructionCleanupSheet(
    before: String,
    after: String,
    onApply: () -> Unit,
    onDismiss: () -> Unit,
) {
    val hasChanges = after != before
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CleanupHeader(stringResource(if (hasChanges) R.string.recipe_apply_cleanup else R.string.recipe_nothing_to_clean))
                if (!hasChanges) {
                    Text(stringResource(R.string.recipe_instructions_look_good))
                } else {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary),
                    ) {
                        Text(after, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(14.dp).fillMaxWidth())
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (hasChanges) {
                    OutlinedButton(
                        onClick = onDismiss,
                        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                    ) { Text(stringResource(R.string.recipe_keep_mine)) }
                    Button(onClick = onApply, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                        Text(stringResource(R.string.apply))
                    }
                } else {
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        }
    }
}

/** Save button for the top bar of screens that host a [RecipeForm]. */
@Composable
fun RecipeFormSaveButton(
    vm: RecipeFormViewModel,
    label: String = stringResource(R.string.save),
    onSubmit: (NewRecipe) -> Unit,
) {
    Button(
        onClick = { vm.submit(onSubmit) },
        modifier = Modifier.padding(end = 8.dp),
    ) { Text(label) }
}

private val CalorieConfidence.label: Int
    get() = when (this) {
        CalorieConfidence.HIGH -> R.string.calories_confidence_high
        CalorieConfidence.MEDIUM -> R.string.calories_confidence_medium
        CalorieConfidence.LOW -> R.string.calories_confidence_low
    }
