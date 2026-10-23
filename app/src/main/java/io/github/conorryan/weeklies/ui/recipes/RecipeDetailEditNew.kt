package io.github.conorryan.weeklies.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.data.import.isValidServings
import io.github.conorryan.weeklies.data.import.rescaleToSinglePortion
import io.github.conorryan.weeklies.data.model.NewRecipe
import io.github.conorryan.weeklies.data.model.Recipe
import io.github.conorryan.weeklies.ui.ConfirmDialog
import io.github.conorryan.weeklies.ui.ModalScreen
import kotlinx.coroutines.launch

@Composable
fun RecipeDetailScreen(
    graph: AppGraph,
    recipeId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
) {
    // Warm app state: the recipe list is already loaded by the time the user
    // can navigate here, so the first composed frame has real content and no
    // placeholder flashes.
    val recipes by graph.recipesState.collectAsStateWithLifecycle()
    val current = recipes.firstOrNull { it.id == recipeId }
    val scope = rememberCoroutineScope()
    var adjustPortionsOpen by remember { mutableStateOf(false) }

    ModalScreen(title = current?.name ?: "Recipe", onBack = onBack) {
        if (current != null) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(current.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Row {
                    TextButton(onClick = { onEdit(current.id) }) { Text("Edit recipe") }
                    TextButton(onClick = { adjustPortionsOpen = true }) { Text("Adjust portions") }
                }
                Text(
                    "Ingredients (single portion)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                for (line in current.ingredients) {
                    Text("- $line", style = MaterialTheme.typography.bodyLarge)
                }
                if (current.instructions.isNotEmpty()) {
                    Text(
                        "Instructions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(current.instructions, style = MaterialTheme.typography.bodyLarge)
                }
                TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Back to recipes")
                }
            }
            if (adjustPortionsOpen) {
                AdjustPortionsDialog(
                    recipe = current,
                    onRescale = { scaled ->
                        scope.launch {
                            graph.recipeRepository.updateRecipe(
                                current.id,
                                NewRecipe(
                                    name = current.name,
                                    ingredients = scaled,
                                    instructions = current.instructions,
                                ),
                            )
                        }
                        adjustPortionsOpen = false
                    },
                    onDismiss = { adjustPortionsOpen = false },
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize().padding(top = 12.dp),
            ) {
                Text("Recipe not found", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("The recipe may have been deleted.")
                TextButton(onClick = onBack) { Text("Back to recipes") }
            }
        }
    }
}

/**
 * Asks how many portions the currently listed amounts make and rescales them
 * to a single portion, with a live preview of the resulting lines.
 */
@Composable
private fun AdjustPortionsDialog(
    recipe: Recipe,
    onRescale: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var portionsText by remember { mutableStateOf("1") }
    val portions = portionsText.trim().toDoubleOrNull()
    val portionsValid = portions != null && isValidServings(portions) && portions != 1.0
    val preview = if (portions != null && isValidServings(portions)) {
        rescaleToSinglePortion(recipe.ingredients, portions)
    } else {
        emptyList()
    }

    ConfirmDialog(
        title = recipe.name,
        confirmLabel = "Rescale",
        confirmEnabled = portionsValid,
        onConfirm = { onRescale(preview) },
        onDismiss = onDismiss,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "The listed amounts make how many portions? " +
                        "Amounts are rescaled to a single portion (use 0.5 to double).",
                )
                OutlinedTextField(
                    value = portionsText,
                    onValueChange = { portionsText = it },
                    label = { Text("Portions") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (portionsValid) {
                    Text(
                        "New amounts",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        for (line in preview) {
                            Text("- $line")
                        }
                    }
                }
            }
        },
    )
}

@Composable
fun RecipeEditScreen(
    graph: AppGraph,
    recipeId: String,
    onBack: () -> Unit,
) {
    // Warm app state: see RecipeDetailScreen.
    val recipes by graph.recipesState.collectAsStateWithLifecycle()
    val current = recipes.firstOrNull { it.id == recipeId }
    val scope = rememberCoroutineScope()

    ModalScreen(title = if (current != null) "Edit recipe" else "New recipe", onBack = onBack) {
        if (current != null) {
            RecipeForm(
                initialRecipe = io.github.conorryan.weeklies.data.model.NewRecipe(
                    name = current.name,
                    ingredients = current.ingredients,
                    instructions = current.instructions,
                ),
                graph = graph,
            ) { next ->
                scope.launch { graph.recipeRepository.updateRecipe(current.id, next) }
                onBack()
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                Text("Recipe not found", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("The recipe may have been deleted.")
            }
        }
    }
}

@Composable
fun RecipeNewScreen(
    graph: AppGraph,
    onBack: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    ModalScreen(title = "New recipe", onBack = onBack) {
        RecipeForm(initialRecipe = null) { recipe ->
            scope.launch { graph.recipeRepository.addRecipe(recipe) }
            onBack()
        }
    }
}
