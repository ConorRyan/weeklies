package io.github.conorryan.weeklies.ui.recipes

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import io.github.conorryan.weeklies.ui.ConfirmDialog
import io.github.conorryan.weeklies.ui.InfoDialog
import io.github.conorryan.weeklies.ui.RecipeTile
import io.github.conorryan.weeklies.ui.ScreenTitle
import io.github.conorryan.weeklies.ui.SearchField
import io.github.conorryan.weeklies.ui.weekdayName

private fun openWebsite(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        // No browser installed to handle the link.
    }
}

/** Selected recipe ids for multiselect delete; a Set so toggling is idempotent. */
private val SelectionSaver = Saver<Set<String>, List<String>>(
    save = { it.toList() },
    restore = { it.toSet() },
)

@Composable
fun RecipesScreen(
    graph: AppGraph,
    onNew: () -> Unit,
    onImport: () -> Unit,
    onOpenRecipe: (String) -> Unit,
) {
    val vm: RecipesViewModel = viewModel(factory = RecipesViewModel.Factory(graph))
    val recipes by graph.recipesState.collectAsStateWithLifecycle()
    val byDay by graph.planState.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var showRecommendedSites by remember { mutableStateOf(false) }
    var selection by rememberSaveable(stateSaver = SelectionSaver) { mutableStateOf(emptySet<String>()) }
    val selectionMode = selection.isNotEmpty()
    var deleteOpen by remember { mutableStateOf(false) }
    val shown = filterRecipes(recipes, query)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                ScreenTitle(title = stringResource(R.string.tab_recipes))
            }
            if (recipes.isNotEmpty()) {
                item {
                    SearchField(
                        query = query,
                        onQueryChange = { query = it },
                        placeholder = stringResource(R.string.recipe_search_name_or_ingredient),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                    )
                }
            }
            if (recipes.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.recipe_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            } else if (shown.isEmpty()) {
                item {
                    Text(
                        stringResource(R.string.recipe_search_no_match),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }
            items(shown, key = { it.id }) { recipe ->
                RecipeCard(
                    recipe = recipe,
                    days = plannedDays(byDay, recipe.id),
                    selected = recipe.id in selection,
                    selectionMode = selectionMode,
                    onClick = {
                        if (selectionMode) {
                            selection = if (recipe.id in selection) selection - recipe.id else selection + recipe.id
                        } else {
                            onOpenRecipe(recipe.id)
                        }
                    },
                    onLongClick = { if (!selectionMode) selection = setOf(recipe.id) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
            item {
                TextButton(
                    onClick = { showRecommendedSites = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                ) {
                    Icon(Icons.Outlined.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.recipe_recommended_sites), modifier = Modifier.padding(start = 8.dp))
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (selectionMode) {
                Text(
                    pluralStringResource(R.plurals.recipe_selected_count, selection.size, selection.size),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                        .padding(start = 4.dp),
                )
                ExtendedFloatingActionButton(
                    onClick = { selection = emptySet() },
                    icon = { Icon(Icons.Outlined.Done, contentDescription = null) },
                    text = { Text(stringResource(R.string.recipe_selection_done)) },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
                )
                ExtendedFloatingActionButton(
                    onClick = { deleteOpen = true },
                    icon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                    text = { Text(stringResource(R.string.recipe_delete_selected, selection.size)) },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                )
            } else {
                ExtendedFloatingActionButton(
                    onClick = onImport,
                    icon = { Icon(Icons.Outlined.Link, contentDescription = null) },
                    text = { Text(stringResource(R.string.recipe_import)) },
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
                    modifier = Modifier.weight(1f, fill = false),
                )
                ExtendedFloatingActionButton(
                    onClick = onNew,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.recipe_create_new)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }

    if (showRecommendedSites) {
        RecommendedSitesDialog(onDismiss = { showRecommendedSites = false })
    }

    if (deleteOpen && selection.isNotEmpty()) {
        ConfirmDialog(
            title = stringResource(R.string.recipe_delete_selected_confirm_title),
            confirmLabel = stringResource(R.string.delete),
            destructive = true,
            onConfirm = {
                deleteOpen = false
                vm.removeRecipes(selection)
                selection = emptySet()
            },
            onDismiss = { deleteOpen = false },
            text = {
                Text(pluralStringResource(R.plurals.recipe_delete_selected_confirm_text, selection.size, selection.size))
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecipeCard(
    recipe: Recipe,
    days: List<Weekday>,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = stringResource(R.string.recipe_select_long_press),
            ),
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (selectionMode) {
                // The whole card toggles; the checkbox only shows the state.
                Checkbox(checked = selected, onCheckedChange = null)
            } else {
                RecipeTile(recipe.name, size = 52)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    recipe.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            pluralStringResource(R.plurals.recipe_ingredient_count, recipe.ingredients.size, recipe.ingredients.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        for (day in days) {
                            Text(
                                weekdayName(day, short = true),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.extraLarge)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                            )
                        }
                    }
                    // The saved per-portion estimate, tucked into the corner; set only in the edit form.
                    if (recipe.calories != null) {
                        Icon(
                            Icons.Outlined.LocalFireDepartment,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            stringResource(R.string.calories_kcal_value, recipe.calories),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** Recipe websites that import cleanly; tapping one opens it in the browser. */
@Composable
fun RecommendedSitesDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    InfoDialog(
        title = stringResource(R.string.recipe_recommended_sites),
        onDismiss = onDismiss,
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                for (site in RecommendedSites) {
                    Text(
                        site.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onDismiss()
                                openWebsite(context, site.url)
                            }
                            .heightIn(min = 44.dp)
                            .padding(vertical = 10.dp),
                    )
                }
            }
        },
    )
}
