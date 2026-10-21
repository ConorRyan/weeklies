package io.github.conorryan.weeklies.ui.recipes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.ui.ModalScreen
import kotlinx.coroutines.launch

@Composable
fun RecipeDetailScreen(
    graph: AppGraph,
    recipeId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val recipe by graph.recipeRepository.observeRecipe(recipeId).collectAsStateWithLifecycle(null)
    val current = recipe

    ModalScreen(title = current?.name ?: "Recipe", onBack = onBack) {
        if (current != null) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(current.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = { onEdit(current.id) }) { Text("Edit recipe") }
                Text(
                    "Ingredients (single portion)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                for (line in current.ingredients) {
                    Text("- $line", style = MaterialTheme.typography.bodyLarge)
                }
                TextButton(onClick = onBack, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Back to recipes")
                }
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

@Composable
fun RecipeEditScreen(
    graph: AppGraph,
    recipeId: String,
    onBack: () -> Unit,
) {
    val recipe by graph.recipeRepository.observeRecipe(recipeId).collectAsStateWithLifecycle(null)
    val current = recipe
    val scope = rememberCoroutineScope()

    ModalScreen(title = if (current != null) "Edit recipe" else "New recipe", onBack = onBack) {
        if (current != null) {
            RecipeForm(
                initialRecipe = io.github.conorryan.weeklies.data.model.NewRecipe(
                    name = current.name,
                    ingredients = current.ingredients,
                ),
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
