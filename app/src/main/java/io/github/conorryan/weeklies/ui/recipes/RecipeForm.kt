package io.github.conorryan.weeklies.ui.recipes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.conorryan.weeklies.data.model.NewRecipe

/**
 * Shared recipe entry form for new, edit, and import-review flows.
 * Mirrors the RN RecipeAddForm: name, editable ingredient rows, validation.
 */
@Composable
fun RecipeForm(
    initialRecipe: NewRecipe?,
    submitLabel: String = "Save recipe",
    onAddRecipe: (NewRecipe) -> Unit,
) {
    var name by remember { mutableStateOf(initialRecipe?.name ?: "") }
    val ingredients = remember {
        mutableStateListOf<String>().apply {
            if (!initialRecipe?.ingredients.isNullOrEmpty()) addAll(initialRecipe!!.ingredients)
            else add("")
        }
    }
    var instructions by remember { mutableStateOf(initialRecipe?.instructions ?: "") }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Name", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("Recipe name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            "Ingredients",
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
        )
        for (index in ingredients.indices) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = ingredients[index],
                    onValueChange = { ingredients[index] = it },
                    placeholder = { Text("Ingredient") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { if (ingredients.size > 1) ingredients.removeAt(index) },
                    enabled = ingredients.size > 1,
                ) {
                    Icon(
                        Icons.Filled.Remove,
                        contentDescription = "Remove ingredient",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Text(
            "Instructions",
            fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = instructions,
            onValueChange = { instructions = it },
            placeholder = { Text("Steps, notes, timing…") },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { ingredients.add("") }) {
                Text("Add ingredient", fontWeight = FontWeight.SemiBold)
            }
            TextButton(
                onClick = {
                    val trimmedName = name.trim()
                    val cleaned = ingredients.map { it.trim() }.filter { it.isNotEmpty() }
                    when {
                        trimmedName.isEmpty() -> error = "Recipe name is required."
                        cleaned.isEmpty() -> error = "Add at least one ingredient."
                        else -> {
                            error = ""
                            onAddRecipe(
                                NewRecipe(
                                    name = trimmedName,
                                    ingredients = cleaned,
                                    instructions = instructions.trim(),
                                ),
                            )
                        }
                    }
                },
            ) {
                Text(submitLabel, fontWeight = FontWeight.SemiBold)
            }
        }

        if (error.isNotEmpty()) {
            Text(error, color = MaterialTheme.colorScheme.error)
        }
    }
}
