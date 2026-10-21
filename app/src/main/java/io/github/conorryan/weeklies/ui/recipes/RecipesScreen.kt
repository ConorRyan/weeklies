package io.github.conorryan.weeklies.ui.recipes

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.ui.ActionButton
import io.github.conorryan.weeklies.ui.TabHeader
import io.github.conorryan.weeklies.ui.theme.HeaderColor
import io.github.conorryan.weeklies.ui.theme.headerColorFor
import kotlinx.coroutines.launch

@Composable
fun RecipesScreen(
    graph: AppGraph,
    onNew: () -> Unit,
    onImport: () -> Unit,
    onOpenRecipe: (String) -> Unit,
) {
    val recipes by graph.recipeRepository.recipes.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TabHeader("Recipes", Icons.AutoMirrored.Filled.MenuBook, headerColorFor(HeaderColor.RECIPES))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionButton(
                label = "Create new",
                onClick = onNew,
                modifier = Modifier.weight(1f),
            )
            ActionButton(
                label = "Import",
                onClick = onImport,
                modifier = Modifier.weight(1f),
            )
        }

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
