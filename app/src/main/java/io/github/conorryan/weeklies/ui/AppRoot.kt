package io.github.conorryan.weeklies.ui

import androidx.annotation.StringRes
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.conorryan.weeklies.AppGraph
import io.github.conorryan.weeklies.R
import io.github.conorryan.weeklies.ui.import.ImportScreen
import io.github.conorryan.weeklies.ui.plan.PlanScreen
import io.github.conorryan.weeklies.ui.recipes.RecipeDetailScreen
import io.github.conorryan.weeklies.ui.recipes.RecipeEditScreen
import io.github.conorryan.weeklies.ui.recipes.RecipeNewScreen
import io.github.conorryan.weeklies.ui.recipes.RecipesScreen
import io.github.conorryan.weeklies.ui.settings.SettingsScreen
import io.github.conorryan.weeklies.ui.shopping.ShoppingListScreen
import io.github.conorryan.weeklies.ui.support.HelpScreen
import io.github.conorryan.weeklies.ui.support.PrivacyScreen
import io.github.conorryan.weeklies.ui.theme.WeekliesTheme
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

private data class TabSpec(val route: String, @StringRes val label: Int, val icon: ImageVector)

private val TABS = listOf(
    TabSpec(Routes.PLAN, R.string.tab_plan, Icons.Outlined.CalendarMonth),
    TabSpec(Routes.RECIPES, R.string.tab_recipes, Icons.AutoMirrored.Outlined.MenuBook),
    TabSpec(Routes.SHOPPING, R.string.tab_shopping, Icons.Outlined.ShoppingBag),
    TabSpec(Routes.SETTINGS, R.string.tab_settings, Icons.Outlined.Tune),
)

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Keeps every tab screen composed so their state and flows stay warm across
 * switches; only the active tab is measured and drawn, so switching renders
 * fully-loaded content in a single frame.
 */
@Composable
private fun PersistentTabHost(
    activeRoute: String,
    content: @Composable (route: String) -> Unit,
) {
    Layout(
        content = {
            TABS.forEach { tab ->
                key(tab.route) { content(tab.route) }
            }
        },
    ) { measurables, constraints ->
        val index = TABS.indexOfFirst { it.route == activeRoute }.coerceAtLeast(0)
        val placeable = measurables.getOrNull(index)?.measure(constraints)
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable?.place(0, 0)
        }
    }
}

private fun resolve(context: android.content.Context, @StringRes id: Int, arg: String?): String =
    if (arg == null) context.getString(id) else context.getString(id, arg)

@Composable
fun WeekliesApp(graph: AppGraph) {
    val settings by graph.settingsStore.settings.collectAsStateWithLifecycle(graph.settingsStore.defaults)
    val toastBus = remember { ToastBus() }

    WeekliesTheme(colorScheme = settings.colorScheme) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route
        val context = LocalContext.current
        val snackbarHostState = remember { SnackbarHostState() }
        val haptics = LocalHapticFeedback.current
        var toastIsError by remember { mutableStateOf(false) }

        LaunchedEffect(toastBus) {
            toastBus.current
                .onEach { event ->
                    if (event != null) {
                        toastIsError = event.isError
                        val title = resolve(context, event.title, event.titleArg)
                        val message = event.message?.let { resolve(context, it, event.messageArg) }
                        snackbarHostState.showSnackbar(
                            message = if (message != null) "$title: $message" else title,
                            duration = SnackbarDuration.Short,
                        )
                        toastBus.clear()
                    }
                }
                .collect()
        }

        Scaffold(
            snackbarHost = {
                SnackbarHost(snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = if (toastIsError) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.inverseSurface
                        },
                        contentColor = if (toastIsError) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.inverseOnSurface
                        },
                    )
                }
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    for (tab in TABS) {
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                navController.navigateTab(tab.route)
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            },
        ) { padding ->
            // Mirrors the last tab route seen on the back stack; resynced from
            // currentRoute so process-death restore and tab navigation agree.
            // Updated synchronously in composition so a tab switch renders the
            // new tab in the same frame instead of flashing the old one.
            var activeTabRoute by rememberSaveable { mutableStateOf(Routes.PLAN) }
            if (currentRoute != null && TABS.any { it.route == currentRoute }) {
                activeTabRoute = currentRoute
            }
            val onTabRoute = TABS.any { it.route == currentRoute }

            Box(
                Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding)
                    .fillMaxSize(),
            ) {
                PersistentTabHost(activeTabRoute) { route ->
                    when (route) {
                        Routes.PLAN -> PlanScreen(
                            graph = graph,
                            toastBus = toastBus,
                            onGoToRecipes = { navController.navigateTab(Routes.RECIPES) },
                            onNewRecipe = { navController.navigate(Routes.RECIPE_NEW) },
                            onOpenRecipe = { id -> navController.navigate(Routes.recipeDetail(id)) },
                            onOpenHelp = { navController.navigate(Routes.HELP) },
                        )
                        Routes.RECIPES -> RecipesScreen(
                            graph = graph,
                            onNew = { navController.navigate(Routes.RECIPE_NEW) },
                            onImport = { navController.navigate(Routes.RECIPE_IMPORT) },
                            onOpenRecipe = { id -> navController.navigate(Routes.recipeDetail(id)) },
                        )
                        Routes.SHOPPING -> ShoppingListScreen(
                            graph = graph,
                            toastBus = toastBus,
                            onGoToRecipes = { navController.navigateTab(Routes.RECIPES) },
                        )
                        Routes.SETTINGS -> SettingsScreen(
                            graph = graph,
                            toastBus = toastBus,
                            onOpenHelp = { navController.navigate(Routes.HELP) },
                            onOpenPrivacy = { navController.navigate(Routes.PRIVACY) },
                        )
                    }
                }

                Box(
                    Modifier
                        .matchParentSize()
                        .background(
                            if (onTabRoute) {
                                androidx.compose.ui.graphics.Color.Transparent
                            } else {
                                MaterialTheme.colorScheme.background
                            },
                        ),
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Routes.PLAN,
                        enterTransition = { EnterTransition.None },
                        exitTransition = { ExitTransition.None },
                        popEnterTransition = { EnterTransition.None },
                        popExitTransition = { ExitTransition.None },
                    ) {
                        composable(Routes.PLAN) {}
                        composable(Routes.RECIPES) {}
                        composable(Routes.SHOPPING) {}
                        composable(Routes.SETTINGS) {}
                        composable(Routes.RECIPE_NEW) {
                            RecipeNewScreen(graph = graph, onBack = { navController.popBackStack() })
                        }
                        composable(Routes.RECIPE_DETAIL) { entry ->
                            RecipeDetailScreen(
                                graph = graph,
                                recipeId = entry.arguments?.getString("id") ?: "",
                                onBack = { navController.popBackStack() },
                                onEdit = { id -> navController.navigate(Routes.recipeEdit(id)) },
                                toastBus = toastBus,
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
                            HelpScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Routes.PRIVACY) {
                            PrivacyScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}
