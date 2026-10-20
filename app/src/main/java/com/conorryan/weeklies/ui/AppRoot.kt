package com.conorryan.weeklies.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.conorryan.weeklies.AppGraph
import com.conorryan.weeklies.ui.import.ImportScreen
import com.conorryan.weeklies.ui.plan.PlanScreen
import com.conorryan.weeklies.ui.recipes.RecipeDetailScreen
import com.conorryan.weeklies.ui.recipes.RecipeEditScreen
import com.conorryan.weeklies.ui.recipes.RecipeNewScreen
import com.conorryan.weeklies.ui.recipes.RecipesScreen
import com.conorryan.weeklies.ui.settings.SettingsScreen
import com.conorryan.weeklies.ui.shopping.ShoppingListScreen
import com.conorryan.weeklies.ui.support.HelpScreen
import com.conorryan.weeklies.ui.support.PrivacyScreen
import com.conorryan.weeklies.ui.theme.WeekliesTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach

object Routes {
    const val PLAN = "plan"
    const val RECIPES = "recipes"
    const val SHOPPING = "shopping"
    const val SETTINGS = "settings"
    const val RECIPE_NEW = "recipe/new"
    const val RECIPE_DETAIL = "recipe/{id}"
    const val RECIPE_EDIT = "recipe/{id}/edit"
    const val RECIPE_IMPORT = "recipe/import"
    const val HELP = "help"
    const val PRIVACY = "privacy"

    fun recipeDetail(id: String) = "recipe/$id"
    fun recipeEdit(id: String) = "recipe/$id/edit"
}

private data class TabSpec(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    TabSpec(Routes.PLAN, "Weeklies", Icons.Filled.CalendarMonth),
    TabSpec(Routes.RECIPES, "Recipes", Icons.AutoMirrored.Filled.MenuBook),
    TabSpec(Routes.SHOPPING, "Shopping List", Icons.Filled.ShoppingCart),
    TabSpec(Routes.SETTINGS, "Settings", Icons.Filled.Settings),
)

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun WeekliesApp(graph: AppGraph) {
    val settings by graph.settingsStore.settings.collectAsStateWithLifecycle(
        com.conorryan.weeklies.data.model.AppSettings(
            colorScheme = com.conorryan.weeklies.data.model.AppColorScheme.DARK,
            portionsPerDay = 2,
        ),
    )
    val toastBus = remember { ToastBus() }

    WeekliesTheme(colorScheme = settings.colorScheme) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val snackbarHostState = remember { SnackbarHostState() }
        val haptics = LocalHapticFeedback.current

        LaunchedEffect(toastBus) {
            toastBus.current
                .onEach { event ->
                    if (event != null) {
                        snackbarHostState.showSnackbar(
                            message = if (event.message != null) {
                                "${event.title}: ${event.message}"
                            } else {
                                event.title
                            },
                            duration = SnackbarDuration.Short,
                        )
                        toastBus.clear()
                    }
                }
                .collect()
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar {
                    for (tab in TABS) {
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                navController.navigateTab(tab.route)
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.PLAN,
                modifier = Modifier.padding(padding),
            ) {
                composable(Routes.PLAN) {
                    PlanScreen(
                        graph = graph,
                        onGoToRecipes = { navController.navigateTab(Routes.RECIPES) },
                    )
                }
                composable(Routes.RECIPES) {
                    RecipesScreen(
                        graph = graph,
                        onNew = { navController.navigate(Routes.RECIPE_NEW) },
                        onImport = { navController.navigate(Routes.RECIPE_IMPORT) },
                        onOpenRecipe = { id -> navController.navigate(Routes.recipeDetail(id)) },
                    )
                }
                composable(Routes.SHOPPING) { ShoppingListScreen(graph, toastBus) }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        graph = graph,
                        toastBus = toastBus,
                        onOpenHelp = { navController.navigate(Routes.HELP) },
                    )
                }
                composable(Routes.RECIPE_NEW) {
                    RecipeNewScreen(graph = graph, onBack = { navController.popBackStack() })
                }
                composable(Routes.RECIPE_DETAIL) { entry ->
                    RecipeDetailScreen(
                        graph = graph,
                        recipeId = entry.arguments?.getString("id") ?: "",
                        onBack = { navController.popBackStack() },
                        onEdit = { id -> navController.navigate(Routes.recipeEdit(id)) },
                    )
                }
                composable(Routes.RECIPE_EDIT) { entry ->
                    RecipeEditScreen(
                        graph = graph,
                        recipeId = entry.arguments?.getString("id") ?: "",
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Routes.RECIPE_IMPORT) {
                    ImportScreen(
                        graph = graph,
                        onBack = { navController.popBackStack() },
                        onOpenSettings = { navController.navigateTab(Routes.SETTINGS) },
                    )
                }
                composable(Routes.HELP) {
                    HelpScreen(
                        onBack = { navController.popBackStack() },
                        onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                    )
                }
                composable(Routes.PRIVACY) {
                    PrivacyScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
