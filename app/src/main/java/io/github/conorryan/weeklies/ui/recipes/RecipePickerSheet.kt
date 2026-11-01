package io.github.conorryan.weeklies.ui.recipes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.conorryan.weeklies.R
import io.github.conorryan.weeklies.data.model.Recipe
import io.github.conorryan.weeklies.ui.Eyebrow
import io.github.conorryan.weeklies.ui.RecipeTile
import io.github.conorryan.weeklies.ui.SearchField

/**
 * Bottom sheet listing recipes with a search field. [selectedId] gets a tick;
 * [footer] holds the sheet's own actions (clear the day, new recipe…), and
 * [emptyContent] replaces the list when there are no recipes at all.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipePickerSheet(
    title: String,
    recipes: List<Recipe>,
    onPick: (Recipe) -> Unit,
    onDismiss: () -> Unit,
    eyebrow: String? = null,
    selectedId: String? = null,
    emptyContent: @Composable () -> Unit = {},
    footer: (@Composable RowScope.() -> Unit)? = null,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by rememberSaveable { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    if (eyebrow != null) Eyebrow(eyebrow)
                    Text(title, style = MaterialTheme.typography.headlineSmall)
                }
                FilledTonalButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
            }

            if (recipes.isEmpty()) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) { emptyContent() }
            } else {
                SearchField(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = stringResource(R.string.recipe_search_hint),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
                val shown = filterRecipes(recipes, query)
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 460.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    items(shown, key = { it.id }) { recipe ->
                        val selected = recipe.id == selectedId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (selected) MaterialTheme.colorScheme.surfaceContainerLow else androidx.compose.ui.graphics.Color.Transparent,
                                    MaterialTheme.shapes.small,
                                )
                                .clickable { onPick(recipe) }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            RecipeTile(recipe.name, size = 40)
                            Column(Modifier.weight(1f)) {
                                Text(recipe.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(
                                    pluralStringResource(R.plurals.plan_ingredient_count, recipe.ingredients.size, recipe.ingredients.size),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (selected) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = stringResource(R.string.selected),
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                    if (shown.isEmpty()) {
                        item {
                            Text(
                                stringResource(R.string.recipe_search_no_match),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                }
            }

            if (footer != null) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(top = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    content = footer,
                )
            }
        }
    }
}
