package io.github.conorryan.weeklies.ui.plan

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.R
import io.github.conorryan.weeklies.data.model.Recipe
import io.github.conorryan.weeklies.data.model.Weekday
import io.github.conorryan.weeklies.ui.CardDivider
import io.github.conorryan.weeklies.ui.CardGroup
import io.github.conorryan.weeklies.ui.Eyebrow
import io.github.conorryan.weeklies.ui.ScreenTitle
import io.github.conorryan.weeklies.ui.ToastBus
import io.github.conorryan.weeklies.ui.recipes.RecipePickerSheet
import io.github.conorryan.weeklies.ui.shopping.PortionsDialog
import io.github.conorryan.weeklies.ui.weekdayName

@Composable
fun PlanScreen(
    graph: AppGraph,
    toastBus: ToastBus,
    onGoToRecipes: () -> Unit,
    onNewRecipe: () -> Unit,
    onOpenRecipe: (String) -> Unit,
    onOpenHelp: () -> Unit,
) {
    val vm: PlanViewModel = viewModel(factory = PlanViewModel.Factory(graph, toastBus))
    val state by vm.state.collectAsStateWithLifecycle()
    var pickerDay by remember { mutableStateOf<Weekday?>(null) }
    var portionsFor by remember { mutableStateOf<Recipe?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenTitle(
            title = stringResource(R.string.plan_title),
            eyebrow = stringResource(R.string.plan_eyebrow),
            action = {
                OutlinedIconButton(
                    onClick = onOpenHelp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = stringResource(R.string.help))
                }
            },
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TodayCard(
                today = state.today,
                assigned = state.byDay[state.today] != null,
                recipe = state.recipeFor(state.today),
                defaultPortions = state.defaultPortions,
                onChange = { pickerDay = state.today },
                onOpen = { recipe -> onOpenRecipe(recipe.id) },
                onAddToList = { recipe -> portionsFor = recipe },
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Eyebrow(stringResource(R.string.plan_rest_of_week), Modifier.padding(horizontal = 4.dp))
                CardGroup {
                    state.restOfWeek.forEachIndexed { index, day ->
                        if (index > 0) CardDivider()
                        DayRow(
                            day = day,
                            assigned = state.byDay[day] != null,
                            recipe = state.recipeFor(day),
                            onClick = { pickerDay = day },
                        )
                    }
                }
            }
        }
    }

    val day = pickerDay
    if (day != null) {
        RecipePickerSheet(
            eyebrow = stringResource(R.string.plan_every),
            title = weekdayName(day),
            recipes = state.recipes,
            selectedId = state.byDay[day],
            onPick = { recipe ->
                vm.setDayRecipe(day, recipe.id)
                pickerDay = null
            },
            onDismiss = { pickerDay = null },
            emptyContent = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.plan_add_recipes_first))
                    Button(onClick = { pickerDay = null; onGoToRecipes() }) {
                        Text(stringResource(R.string.plan_go_to_recipes))
                    }
                }
            },
            footer = {
                OutlinedButton(
                    onClick = {
                        vm.setDayRecipe(day, null)
                        pickerDay = null
                    },
                    enabled = state.byDay[day] != null,
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.plan_leave_empty)) }
                Button(
                    onClick = { pickerDay = null; onNewRecipe() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.recipe_new), modifier = Modifier.padding(start = 6.dp))
                }
            },
        )
    }

    val target = portionsFor
    if (target != null) {
        PortionsDialog(
            recipe = target,
            defaultPortions = state.defaultPortions,
            onAdd = { portions ->
                vm.addToShoppingList(target, portions)
                portionsFor = null
            },
            onDismiss = { portionsFor = null },
        )
    }
}

@Composable
private fun TodayCard(
    today: Weekday,
    assigned: Boolean,
    recipe: Recipe?,
    defaultPortions: Int,
    onChange: () -> Unit,
    onOpen: (Recipe) -> Unit,
    onAddToList: (Recipe) -> Unit,
) {
    val onCard = MaterialTheme.colorScheme.onPrimary
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = onCard,
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.plan_today_label, weekdayName(today)).uppercase(),
                    style = io.github.conorryan.weeklies.ui.theme.EyebrowStyle,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .background(onCard, MaterialTheme.shapes.extraLarge)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Row(Modifier.weight(1f)) {}
                // The empty state has its own big "Choose recipe" button.
                if (recipe != null) {
                    TextButton(
                        onClick = onChange,
                        colors = ButtonDefaults.textButtonColors(contentColor = onCard),
                    ) { Text(stringResource(R.string.plan_change)) }
                }
            }

            if (recipe == null) {
                Text(
                    stringResource(if (assigned) R.string.plan_recipe_removed else R.string.plan_today_empty),
                    style = MaterialTheme.typography.headlineMedium,
                )
                Button(
                    onClick = onChange,
                    colors = ButtonDefaults.buttonColors(containerColor = onCard, contentColor = MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.plan_choose_recipe)) }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(recipe.name, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        pluralStringResource(R.plurals.plan_ingredient_count, recipe.ingredients.size, recipe.ingredients.size) +
                            " · " +
                            pluralStringResource(R.plurals.plan_portions_count, defaultPortions, defaultPortions),
                        style = MaterialTheme.typography.bodyMedium,
                        color = onCard.copy(alpha = 0.85f),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onAddToList(recipe) },
                        colors = ButtonDefaults.buttonColors(containerColor = onCard, contentColor = MaterialTheme.colorScheme.onSurface),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Outlined.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.plan_add_to_list), modifier = Modifier.padding(start = 8.dp))
                    }
                    OutlinedIconButton(
                        onClick = { onOpen(recipe) },
                        border = BorderStroke(1.5.dp, onCard),
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = stringResource(R.string.plan_open_recipe),
                            tint = onCard,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayRow(
    day: Weekday,
    assigned: Boolean,
    recipe: Recipe?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            weekdayName(day, short = true),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(44.dp),
        )
        when {
            recipe != null -> Text(
                recipe.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            assigned -> Text(
                stringResource(R.string.plan_recipe_removed_choose),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            else -> Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                Text(
                    stringResource(R.string.plan_choose_recipe),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
        )
    }
}
