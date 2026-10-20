package com.conorryan.weeklies.ui.recipes

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
import com.conorryan.weeklies.AppGraph
import com.conorryan.weeklies.ui.ModalScreen
import kotlinx.coroutines.launch

@Composable
fun RecipeDetailScreen(
    graph: AppGraph,
    recipeId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val recipes by graph.recipeRepository.recipes.collectAsStateWithLifecycle(emptyList())
    val recipe = recipes.firstOrNull { it.id == recipeId }

    ModalScreen(title = recipe?.name ?: "Recipe", onBack = onBack) {
        if (recipe != null) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(recipe.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = { onEdit(recipe.id) }) { Text("Edit recipe") }
                Text(
                    "Ingredients (single portion)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                for (line in recipe.ingredients) {
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
    val recipes by graph.recipeRepository.recipes.collectAsStateWithLifecycle(emptyList())
    val recipe = recipes.firstOrNull { it.id == recipeId }
    val scope = rememberCoroutineScope()

    ModalScreen(title = if (recipe != null) "Edit recipe" else "New recipe", onBack = onBack) {
        if (recipe != null) {
            RecipeForm(
                initialRecipe = com.conorryan.weeklies.data.model.NewRecipe(
                    name = recipe.name,
                    ingredients = recipe.ingredients,
                ),
            ) { next ->
                scope.launch { graph.recipeRepository.updateRecipe(recipe.id, next) }
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
