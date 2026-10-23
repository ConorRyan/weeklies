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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.ui.BottomAction
import io.github.conorryan.weeklies.ui.BottomActionBar
import io.github.conorryan.weeklies.ui.TabHeader
import io.github.conorryan.weeklies.ui.theme.HeaderColor
import io.github.conorryan.weeklies.ui.theme.headerColorFor
import kotlinx.coroutines.launch

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
    val scope = rememberCoroutineScope()
    var deleteTarget by remember { mutableStateOf<String?>(null) }
    var showRecommendedSites by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabHeader("Recipes", Icons.AutoMirrored.Filled.MenuBook, headerColorFor(HeaderColor.RECIPES))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            if (recipes.isEmpty()) {
                Text(
                    "No recipes yet.",
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
                                "${recipe.ingredients.size} ingredient${if (recipe.ingredients.size == 1) "" else "s"}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    IconButton(onClick = { deleteTarget = recipe.id }) {
                        Icon(
                            Icons.Filled.Remove,
                            contentDescription = "Delete ${recipe.name}",
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
                    label = "Create new",
                    onClick = onNew,
                ),
                BottomAction(
                    icon = Icons.Filled.Download,
                    label = "Import",
                    onClick = onImport,
                ),
                BottomAction(
                    icon = Icons.Filled.Public,
                    label = "Websites",
                    onClick = { showRecommendedSites = true },
                ),
            ),
        )
    }

    if (showRecommendedSites) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { showRecommendedSites = false },
            title = { Text("Recommended websites") },
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
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = { showRecommendedSites = false }) { Text("Close") }
            },
        )
    }

    val target = deleteTarget
    val targetRecipe = recipes.firstOrNull { it.id == target }
    if (target != null && targetRecipe != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete recipe?") },
            text = { Text("Remove \"${targetRecipe.name}\"? This cannot be undone.") },
            confirmButton = {
                OutlinedButton(
                    onClick = {
                        scope.launch { graph.recipeRepository.removeRecipe(target) }
                        deleteTarget = null
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }
}
