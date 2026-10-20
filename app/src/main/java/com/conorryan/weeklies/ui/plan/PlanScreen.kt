package com.conorryan.weeklies.ui.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.conorryan.weeklies.AppGraph
import com.conorryan.weeklies.data.model.Weekday
import com.conorryan.weeklies.data.model.emptyByDay
import com.conorryan.weeklies.ui.TabHeader
import com.conorryan.weeklies.ui.theme.HeaderColor
import com.conorryan.weeklies.ui.theme.headerColorFor
import kotlinx.coroutines.launch
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanScreen(
    graph: AppGraph,
    onGoToRecipes: () -> Unit,
) {
    val recipes by graph.recipeRepository.recipes.collectAsStateWithLifecycle(emptyList())
    val byDay by graph.planRepository.byDay.collectAsStateWithLifecycle(emptyByDay())
    var pickerDay by remember { mutableStateOf<Weekday?>(null) }
    val today = remember { Weekday.fromDate(Date()) }
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TabHeader("Weeklies", Icons.Filled.CalendarMonth, headerColorFor(HeaderColor.PLAN))
        Text(
            "Tap a row to assign a recipe. Repeats every week.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        for (day in Weekday.entries) {
            val recipeId = byDay[day]
            val recipe = recipeId?.let { id -> recipes.firstOrNull { it.id == id } }
            val line = when {
                recipeId == null -> "Choose recipe"
                recipe == null -> "Recipe removed"
                else -> recipe.name
            }
            val isToday = day == today

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { pickerDay = day }
                    .background(
                        if (isToday) headerColorFor(HeaderColor.PLAN).copy(alpha = 0.14f)
                        else androidx.compose.ui.graphics.Color.Transparent,
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    day.label,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.align(Alignment.CenterVertically),
                )
                Text(
                    line,
                    color = if (recipeId == null || recipe == null) {
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f)
                        .align(Alignment.CenterVertically),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                )
            }
        }
        Box(Modifier.padding(bottom = 24.dp))
    }

    val day = pickerDay
    if (day != null) {
        ModalBottomSheet(
            onDismissRequest = { pickerDay = null },
            sheetState = sheetState,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(day.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    "Done",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { pickerDay = null },
                )
            }

            if (byDay[day] != null) {
                Text(
                    "Clear assignment",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clickable {
                            scope.launch { graph.planRepository.setDayRecipe(day, null) }
                            pickerDay = null
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            if (recipes.isEmpty()) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Add recipes first, then pick one for each day.")
                    Text(
                        "Go to Recipes",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            pickerDay = null
                            onGoToRecipes()
                        },
                    )
                }
            } else {
                for (item in recipes) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                scope.launch { graph.planRepository.setDayRecipe(day, item.id) }
                                pickerDay = null
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(item.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${item.ingredients.size} ingredient${if (item.ingredients.size == 1) "" else "s"}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
            Box(Modifier.padding(bottom = 32.dp))
        }
    }
}
