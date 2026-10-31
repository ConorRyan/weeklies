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
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.R
import io.github.conorryan.weeklies.data.import.isValidServings
import io.github.conorryan.weeklies.data.import.rescaleToSinglePortion
import io.github.conorryan.weeklies.data.model.NewRecipe
import io.github.conorryan.weeklies.data.model.Recipe
import io.github.conorryan.weeklies.ui.ConfirmDialog
import io.github.conorryan.weeklies.ui.ModalScreen

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
    val vm: RecipesViewModel = viewModel(factory = RecipesViewModel.Factory(graph))
    var adjustPortionsOpen by remember { mutableStateOf(false) }

    ModalScreen(title = current?.name ?: stringResource(R.string.recipe_default_title), onBack = onBack) {
        if (current != null) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(current.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Row {
                    TextButton(onClick = { onEdit(current.id) }) {
                        Text(stringResource(R.string.recipe_edit))
                    }
                    TextButton(onClick = { adjustPortionsOpen = true }) {
                        Text(stringResource(R.string.recipe_adjust_portions))
                    }
                }
                Text(
                    stringResource(R.string.recipe_ingredients_single_portion),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                for (line in current.ingredients) {
                    Text("- $line", style = MaterialTheme.typography.bodyLarge)
                }
                if (current.instructions.isNotEmpty()) {
                    Text(
                        stringResource(R.string.recipe_instructions_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(current.instructions, style = MaterialTheme.typography.bodyLarge)
                }
                TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
                    Text(stringResource(R.string.recipe_back_to_recipes))
                }
            }
            if (adjustPortionsOpen) {
                AdjustPortionsDialog(
                    recipe = current,
                    onRescale = { scaled ->
                        vm.rescaleRecipe(current.id, scaled)
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
                Text(
                    stringResource(R.string.recipe_not_found_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(stringResource(R.string.recipe_not_found_message))
                TextButton(onClick = onBack) { Text(stringResource(R.string.recipe_back_to_recipes)) }
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
        confirmLabel = stringResource(R.string.recipe_rescale),
        confirmEnabled = portionsValid,
        onConfirm = { onRescale(preview) },
        onDismiss = onDismiss,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.recipe_adjust_portions_text))
                OutlinedTextField(
                    value = portionsText,
                    onValueChange = { portionsText = it },
                    label = { Text(stringResource(R.string.recipe_portions)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (portionsValid) {
                    Text(
                        stringResource(R.string.recipe_new_amounts),
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
    val initial = current?.let { NewRecipe(name = it.name, ingredients = it.ingredients, instructions = it.instructions) }
    val vm: RecipeFormViewModel = viewModel(
        factory = RecipeFormViewModel.editRecipeFactory(graph, recipeId, initial),
    )
    val state by vm.state.collectAsStateWithLifecycle()

    ModalScreen(
        title = if (current != null) stringResource(R.string.recipe_edit) else stringResource(R.string.recipe_new),
        onBack = onBack,
    ) {
        if (current != null) {
            RecipeForm(
                state = state,
                vm = vm,
                submitLabel = stringResource(R.string.recipe_save),
            ) { recipe ->
                vm.save(recipe, onBack)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                Text(
                    stringResource(R.string.recipe_not_found_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(stringResource(R.string.recipe_not_found_message))
            }
        }
    }
}

@Composable
fun RecipeNewScreen(
    graph: AppGraph,
    onBack: () -> Unit,
) {
    val vm: RecipeFormViewModel = viewModel(
        factory = RecipeFormViewModel.newRecipeFactory(graph),
    )
    val state by vm.state.collectAsStateWithLifecycle()

    ModalScreen(title = stringResource(R.string.recipe_new), onBack = onBack) {
        RecipeForm(
            state = state,
            vm = vm,
            submitLabel = stringResource(R.string.recipe_save),
        ) { recipe ->
            vm.save(recipe, onBack)
        }
    }
}
