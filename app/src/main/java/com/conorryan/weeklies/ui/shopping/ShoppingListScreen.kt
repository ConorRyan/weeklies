package com.conorryan.weeklies.ui.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.conorryan.weeklies.AppGraph
import com.conorryan.weeklies.data.import.scaleIngredientLine
import com.conorryan.weeklies.data.model.Weekday
import com.conorryan.weeklies.ui.ActionButton
import com.conorryan.weeklies.ui.TabHeader
import com.conorryan.weeklies.ui.ToastBus
import com.conorryan.weeklies.ui.theme.HeaderColor
import com.conorryan.weeklies.ui.theme.headerColorFor
import kotlinx.coroutines.launch
import java.util.Date

@Composable
fun ShoppingListScreen(graph: AppGraph, toastBus: ToastBus) {
    val items by graph.shoppingRepository.items.collectAsStateWithLifecycle(emptyList())
    val recipes by graph.recipeRepository.recipes.collectAsStateWithLifecycle(emptyList())
    val byDay by graph.planRepository.byDay.collectAsStateWithLifecycle(com.conorryan.weeklies.data.model.emptyByDay())
    val settings by graph.settingsStore.settings.collectAsStateWithLifecycle(
        com.conorryan.weeklies.data.model.AppSettings(
            colorScheme = com.conorryan.weeklies.data.model.AppColorScheme.DARK,
            portionsPerDay = 2,
        ),
    )
    val scope = rememberCoroutineScope()
    val hasChecked = items.any { it.checked }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TabHeader("Shopping List", Icons.Filled.ShoppingCart, headerColorFor(HeaderColor.SHOPPING))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionButton(
                label = "Add today's recipe",
                onClick = {
                    val day = Weekday.fromDate(Date())
                    val recipeId = byDay[day]
                    when {
                        recipeId == null -> toastBus.info(
                            "No recipe today",
                            "Assign a recipe to today on the Weeklies tab first.",
                        )
                        else -> {
                            val recipe = recipes.firstOrNull { it.id == recipeId }
                            if (recipe == null) {
                                toastBus.info(
                                    "Recipe missing",
                                    "The planned recipe was removed. Update Weeklies or Recipes.",
                                )
                            } else {
                                scope.launch {
                                    for (line in recipe.ingredients) {
                                        val trimmed = line.trim()
                                        if (trimmed.isEmpty()) continue
                                        graph.shoppingRepository.addItem(
                                            scaleIngredientLine(trimmed, settings.portionsPerDay.toDouble()),
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.weight(1f),
            )
            ActionButton(
                label = "Remove checked",
                onClick = { scope.launch { graph.shoppingRepository.removeCheckedItems() } },
                enabled = hasChecked,
                borderColor = androidx.compose.ui.graphics.Color(0xFFC62828),
                textColor = androidx.compose.ui.graphics.Color(0xFFC62828),
                modifier = Modifier.weight(1f),
            )
        }

        for (item in items) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Checkbox(
                    checked = item.checked,
                    onCheckedChange = {
                        scope.launch { graph.shoppingRepository.toggleItem(item.id) }
                    },
                    modifier = Modifier.size(32.dp),
                )
                Text(
                    item.label,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (item.checked) TextDecoration.LineThrough else null,
                    color = if (item.checked) {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                )
            }
        }

        var newLabel by remember { mutableStateOf("") }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            OutlinedTextField(
                value = newLabel,
                onValueChange = { newLabel = it },
                placeholder = { Text("Item") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    val trimmed = newLabel.trim()
                    if (trimmed.isNotEmpty()) {
                        scope.launch { graph.shoppingRepository.addItem(trimmed) }
                        newLabel = ""
                    }
                },
            ) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = "Add item",
                    tint = headerColorFor(HeaderColor.SHOPPING),
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}
