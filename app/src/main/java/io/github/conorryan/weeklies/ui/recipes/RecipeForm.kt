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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.data.import.CleanedIngredients
import io.github.conorryan.weeklies.data.import.RecipeImportErrorDisplay
import io.github.conorryan.weeklies.data.import.RecipeImportErrors
import io.github.conorryan.weeklies.data.import.isUnchangedPairing
import io.github.conorryan.weeklies.data.import.pairCleanupForReview
import io.github.conorryan.weeklies.data.model.NewRecipe
import io.github.conorryan.weeklies.ui.ConfirmDialog
import io.github.conorryan.weeklies.ui.InfoDialog
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/** Bold label above a form field. */
@Composable
private fun FormLabel(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
}

/**
 * Shared recipe entry form for new, edit, and import-review flows.
 * Mirrors the RN RecipeAddForm: name, editable ingredient rows, validation.
 *
 * When [graph] is provided (the edit screen), the form also offers the AI
 * cleanup actions for ingredients and instructions, applied to the draft.
 */
@Composable
fun RecipeForm(
    initialRecipe: NewRecipe?,
    submitLabel: String = "Save recipe",
    graph: AppGraph? = null,
    onAddRecipe: (NewRecipe) -> Unit,
) {
    var name by remember { mutableStateOf(initialRecipe?.name ?: "") }
    val ingredients = remember {
        mutableStateListOf<String>().apply {
            if (!initialRecipe?.ingredients.isNullOrEmpty()) addAll(initialRecipe!!.ingredients)
            else add("")
        }
    }
    var instructions by remember { mutableStateOf(initialRecipe?.instructions ?: "") }
    var error by remember { mutableStateOf("") }
    val apiKey by (graph?.secretsStore?.mistralApiKey ?: flowOf(""))
        .collectAsStateWithLifecycle("")
    val scope = rememberCoroutineScope()
    // Which cleanup is running: "ingredients", "instructions", or null.
    var cleanupBusy by remember { mutableStateOf<String?>(null) }
    var ingredientPreview by remember { mutableStateOf<CleanedIngredients?>(null) }
    var instructionPreview by remember { mutableStateOf<String?>(null) }
    var cleanupError by remember { mutableStateOf<RecipeImportErrorDisplay?>(null) }

    fun draftIngredients(): List<String> = ingredients.map { it.trim() }.filter { it.isNotEmpty() }

    fun runCleanup(busyTag: String, work: suspend () -> Unit) {
        if (apiKey.trim().isEmpty()) {
            cleanupError = RecipeImportErrors.format(RecipeImportErrors.mistralKeyMissing())
        } else {
            cleanupBusy = busyTag
            scope.launch {
                try {
                    work()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    // canceled; nothing to show
                } catch (e: Exception) {
                    cleanupError = RecipeImportErrors.format(e)
                } finally {
                    cleanupBusy = null
                }
            }
        }
    }

    val runIngredientCleanup: () -> Unit = {
        val g = graph
        if (g != null) {
            runCleanup("ingredients") {
                ingredientPreview = g.recipeCleaner.cleanIngredients(
                    apiKey.trim(),
                    name.trim(),
                    draftIngredients(),
                )
            }
        }
    }

    val runInstructionCleanup: () -> Unit = {
        val g = graph
        if (g != null) {
            runCleanup("instructions") {
                instructionPreview = g.recipeCleaner.cleanInstructions(
                    apiKey.trim(),
                    name.trim(),
                    draftIngredients(),
                    instructions.trim(),
                )
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FormLabel("Name")
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("Recipe name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        FormLabel("Ingredients")
        for (index in ingredients.indices) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = ingredients[index],
                    onValueChange = { ingredients[index] = it },
                    placeholder = { Text("Ingredient") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { if (ingredients.size > 1) ingredients.removeAt(index) },
                    enabled = ingredients.size > 1,
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
            TextButton(onClick = { ingredients.add("") }) {
                Text("Add ingredient", fontWeight = FontWeight.SemiBold)
            }
            if (graph != null) {
                TextButton(
                    onClick = runIngredientCleanup,
                    enabled = cleanupBusy == null && draftIngredients().isNotEmpty(),
                ) {
                    Text(if (cleanupBusy == "ingredients") "Cleaning up…" else "Clean up ingredients ✨")
                }
            }
        }

        FormLabel("Instructions")
        OutlinedTextField(
            value = instructions,
            onValueChange = { instructions = it },
            placeholder = { Text("Steps, notes, timing…") },
            modifier = Modifier.fillMaxWidth(),
        )
        if (graph != null) {
            TextButton(
                onClick = runInstructionCleanup,
                enabled = cleanupBusy == null && instructions.isNotBlank(),
            ) {
                Text(if (cleanupBusy == "instructions") "Cleaning up…" else "Clean up instructions ✨")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(
                onClick = {
                    val trimmedName = name.trim()
                    val cleaned = ingredients.map { it.trim() }.filter { it.isNotEmpty() }
                    when {
                        trimmedName.isEmpty() -> error = "Recipe name is required."
                        cleaned.isEmpty() -> error = "Add at least one ingredient."
                        else -> {
                            error = ""
                            onAddRecipe(
                                NewRecipe(
                                    name = trimmedName,
                                    ingredients = cleaned,
                                    instructions = instructions.trim(),
                                ),
                            )
                        }
                    }
                },
            ) {
                Text(submitLabel, fontWeight = FontWeight.SemiBold)
            }
        }

        if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }

        val preview = ingredientPreview
        if (preview != null) {
            val pairings = pairCleanupForReview(draftIngredients(), preview)
            val hasChanges = pairings.any { !isUnchangedPairing(it) }
            ConfirmDialog(
                title = if (hasChanges) "Apply cleanup?" else "Nothing to clean up",
                confirmLabel = if (hasChanges) "Apply" else "Close",
                dismissLabel = if (hasChanges) "Cancel" else null,
                onConfirm = {
                    if (hasChanges) {
                        ingredients.clear()
                        ingredients.addAll(preview.lines)
                    }
                    ingredientPreview = null
                },
                onDismiss = { ingredientPreview = null },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        if (!hasChanges) {
                            Text("Your ingredients already look good — no changes suggested.")
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

        val newInstructions = instructionPreview
        if (newInstructions != null) {
            val hasChanges = newInstructions != instructions.trim()
            ConfirmDialog(
                title = if (hasChanges) "Apply cleanup?" else "Nothing to clean up",
                confirmLabel = if (hasChanges) "Apply" else "Close",
                dismissLabel = if (hasChanges) "Cancel" else null,
                onConfirm = {
                    if (hasChanges) {
                        instructions = newInstructions
                    }
                    instructionPreview = null
                },
                onDismiss = { instructionPreview = null },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        if (!hasChanges) {
                            Text("Your instructions already look good — no changes suggested.")
                        }
                        for (line in newInstructions.lines()) {
                            Text(line)
                        }
                    }
                },
            )
        }

        val cleanupErrorDisplay = cleanupError
        if (cleanupErrorDisplay != null) {
            InfoDialog(
                title = cleanupErrorDisplay.title,
                onDismiss = { cleanupError = null },
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
