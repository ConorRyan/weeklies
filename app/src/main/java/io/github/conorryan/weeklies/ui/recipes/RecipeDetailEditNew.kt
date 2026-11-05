package io.github.conorryan.weeklies.ui.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.R
import io.github.conorryan.weeklies.data.model.NewRecipe
import io.github.conorryan.weeklies.data.quantity.scaleIngredientLines
import io.github.conorryan.weeklies.ui.CardDivider
import io.github.conorryan.weeklies.ui.CardGroup
import io.github.conorryan.weeklies.ui.ConfirmDialog
import io.github.conorryan.weeklies.ui.ModalScreen
import io.github.conorryan.weeklies.ui.ToastBus
import io.github.conorryan.weeklies.ui.shopping.PortionsDialog
import io.github.conorryan.weeklies.ui.weekdayName

@Composable
fun RecipeDetailScreen(
    graph: AppGraph,
    recipeId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    toastBus: ToastBus,
) {
    // Warm app state: the recipe list and plan are already loaded by the time
    // the user can navigate here, so the first composed frame has real content
    // and no placeholder flashes.
    val recipes by graph.recipesState.collectAsStateWithLifecycle()
    val byDay by graph.planState.collectAsStateWithLifecycle()
    val settings by graph.settingsStore.settings.collectAsStateWithLifecycle(graph.settingsStore.defaults)
    val current = recipes.firstOrNull { it.id == recipeId }
    val vm: RecipesViewModel = viewModel(factory = RecipesViewModel.Factory(graph, toastBus))

    var scaleOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }
    var portionsOpen by remember { mutableStateOf(false) }
    var viewFactor by rememberSaveable { mutableStateOf(1.0) }
    var viewLabel by rememberSaveable { mutableStateOf<String?>(null) }
    // Amounts before the last "Save to recipe", so the banner can undo it.
    var undoLines by rememberSaveable { mutableStateOf<ArrayList<String>?>(null) }
    var savedLabel by rememberSaveable { mutableStateOf("") }

    if (current == null) {
        ModalScreen(title = null, onBack = onBack) {
            Text(stringResource(R.string.recipe_not_found_title), style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(R.string.recipe_not_found_message))
            TextButton(onClick = onBack) { Text(stringResource(R.string.recipe_back_to_recipes)) }
        }
        return
    }

    ModalScreen(
        title = null,
        onBack = onBack,
        actions = {
            IconButton(onClick = { onEdit(current.id) }) {
                Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.recipe_edit))
            }
            IconButton(onClick = { deleteOpen = true }) {
                Icon(
                    Icons.Outlined.DeleteOutline,
                    contentDescription = stringResource(R.string.recipe_delete_content_desc, current.name),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
        bottomBar = {
            Button(
                onClick = { portionsOpen = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
            ) {
                Icon(Icons.Outlined.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.plan_add_to_list), modifier = Modifier.padding(start = 8.dp))
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(current.name, style = MaterialTheme.typography.displaySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                val days = plannedDays(byDay, current.id)
                if (days.isNotEmpty()) {
                    Text(
                        stringResource(R.string.recipe_every_day, days.map { weekdayName(it) }.joinToString(", ")),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.extraLarge)
                            .padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
                Text(
                    pluralStringResource(R.plurals.recipe_ingredient_count, current.ingredients.size, current.ingredients.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        CardGroup {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.recipe_ingredients_label), style = MaterialTheme.typography.titleMedium)
                    Text(
                        viewLabel?.let { stringResource(R.string.recipe_scaled_caption, it) }
                            ?: stringResource(R.string.recipe_one_portion),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val scaled = viewLabel != null
                Button(
                    onClick = { scaleOpen = true },
                    colors = if (scaled) {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 14.dp, end = 10.dp),
                ) {
                    Text(viewLabel ?: stringResource(R.string.scale_button))
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            // View-only here; the edit form owns setting and estimating calories.
            if (current.calories != null) {
                CaloriesRow(calories = current.calories)
            }
            for (line in scaleIngredientLines(current.ingredients, viewFactor)) {
                CardDivider()
                IngredientLine(line)
            }
            val undo = undoLines
            if (undo != null) {
                CardDivider()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(start = 16.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                    Text(
                        stringResource(R.string.scale_saved_banner, savedLabel),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = {
                        vm.rescaleRecipe(current.id, undo)
                        undoLines = null
                    }) { Text(stringResource(R.string.undo)) }
                }
            }
        }

        val steps = instructionSteps(current.instructions)
        if (steps.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(R.string.recipe_instructions_label),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
                steps.forEachIndexed { index, step ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (steps.size > 1) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.onSurface,
                                contentColor = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(26.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text((index + 1).toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Text(step, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }

    if (scaleOpen) {
        ScaleSheet(
            ingredients = current.ingredients,
            onShow = { choice ->
                viewFactor = choice.factor
                viewLabel = choice.label
                scaleOpen = false
            },
            onSave = { choice ->
                undoLines = ArrayList(current.ingredients)
                savedLabel = choice.label
                vm.rescaleRecipe(current.id, scaleIngredientLines(current.ingredients, choice.factor))
                viewFactor = 1.0
                viewLabel = null
                scaleOpen = false
            },
            onReset = {
                viewFactor = 1.0
                viewLabel = null
                scaleOpen = false
            },
            onDismiss = { scaleOpen = false },
        )
    }

    if (portionsOpen) {
        PortionsDialog(
            recipe = current,
            defaultPortions = settings.portionsPerDay,
            onAdd = { portions ->
                vm.addToShoppingList(current, portions)
                portionsOpen = false
            },
            onDismiss = { portionsOpen = false },
        )
    }

    if (deleteOpen) {
        ConfirmDialog(
            title = stringResource(R.string.recipe_delete_confirm_title),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                deleteOpen = false
                vm.removeRecipe(current.id)
                onBack()
            },
            onDismiss = { deleteOpen = false },
            text = { Text(stringResource(R.string.recipe_delete_confirm_text, current.name)) },
        )
    }
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
    val initial = current?.let { NewRecipe(name = it.name, ingredients = it.ingredients, instructions = it.instructions, calories = it.calories) }
    val vm: RecipeFormViewModel = viewModel(
        factory = RecipeFormViewModel.editRecipeFactory(graph, recipeId, initial),
    )
    val state by vm.state.collectAsStateWithLifecycle()

    ModalScreen(
        title = stringResource(if (current != null) R.string.recipe_edit else R.string.recipe_not_found_title),
        onBack = onBack,
        actions = {
            if (current != null) RecipeFormSaveButton(vm) { recipe -> vm.save(recipe, onBack) }
        },
    ) {
        if (current != null) {
            RecipeForm(state = state, vm = vm)
        } else {
            Text(stringResource(R.string.recipe_not_found_message))
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

    ModalScreen(
        title = stringResource(R.string.recipe_new),
        onBack = onBack,
        actions = { RecipeFormSaveButton(vm) { recipe -> vm.save(recipe, onBack) } },
    ) {
        RecipeForm(state = state, vm = vm)
    }
}
