package io.github.conorryan.weeklies.ui.recipes

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.R
import io.github.conorryan.weeklies.ui.BottomAction
import io.github.conorryan.weeklies.ui.BottomActionBar
import io.github.conorryan.weeklies.ui.ConfirmDialog
import io.github.conorryan.weeklies.ui.InfoDialog
import io.github.conorryan.weeklies.ui.TabHeader
import io.github.conorryan.weeklies.ui.theme.HeaderColor
import io.github.conorryan.weeklies.ui.theme.headerColorFor

private fun openWebsite(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        // No browser installed to handle the link.
    }
}

@Composable
fun RecipesScreen(
    graph: AppGraph,
    onNew: () -> Unit,
    onImport: () -> Unit,
    onOpenRecipe: (String) -> Unit,
) {
    val recipes by graph.recipesState.collectAsStateWithLifecycle()
    val vm: RecipesViewModel = viewModel(factory = RecipesViewModel.Factory(graph))
    var deleteTarget by remember { mutableStateOf<String?>(null) }
    var showRecommendedSites by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabHeader(stringResource(R.string.tab_recipes), Icons.AutoMirrored.Filled.MenuBook, headerColorFor(HeaderColor.RECIPES))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            if (recipes.isEmpty()) {
                Text(
                    stringResource(R.string.recipe_empty),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            for (recipe in recipes) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        onClick = { onOpenRecipe(recipe.id) },
                        modifier = Modifier.weight(1f),
                        color = androidx.compose.ui.graphics.Color.Transparent,
                    ) {
                        Column(Modifier.padding(vertical = 8.dp)) {
                            Text(recipe.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                pluralStringResource(
                                    R.plurals.recipe_ingredient_count,
                                    recipe.ingredients.size,
                                    recipe.ingredients.size,
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(onClick = { deleteTarget = recipe.id }) {
                        Icon(
                            Icons.Filled.Remove,
                            contentDescription = stringResource(R.string.recipe_delete_content_desc, recipe.name),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        BottomActionBar(
            listOf(
                BottomAction(
                    icon = Icons.Filled.Add,
                    label = stringResource(R.string.recipe_create_new),
                    onClick = onNew,
                ),
                BottomAction(
                    icon = Icons.Filled.Download,
                    label = stringResource(R.string.recipe_import),
                    onClick = onImport,
                ),
                BottomAction(
                    icon = Icons.Filled.Public,
                    label = stringResource(R.string.recipe_websites),
                    onClick = { showRecommendedSites = true },
                ),
            ),
        )
    }

    if (showRecommendedSites) {
        val context = LocalContext.current
        InfoDialog(
            title = stringResource(R.string.recipe_recommended_sites),
            onDismiss = { showRecommendedSites = false },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                ) {
                    for (site in RecommendedSites) {
                        Text(
                            site.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showRecommendedSites = false
                                    openWebsite(context, site.url)
                                }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            },
        )
    }

    val target = deleteTarget
    val targetRecipe = recipes.firstOrNull { it.id == target }
    if (target != null && targetRecipe != null) {
        ConfirmDialog(
            title = stringResource(R.string.recipe_delete_confirm_title),
            confirmLabel = stringResource(R.string.delete),
            onConfirm = {
                vm.removeRecipe(target)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
            text = { Text(stringResource(R.string.recipe_delete_confirm_text, targetRecipe.name)) },
        )
    }
}
