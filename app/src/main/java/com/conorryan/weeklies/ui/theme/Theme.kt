package com.conorryan.weeklies.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.conorryan.weeklies.data.model.AppColorScheme

/** Brand blue from the plan header. */
val PlanHeaderBlue = Color(0xFF1DA1FF)
val PlanHeaderBlueDark = Color(0xFF0F6FC1)

val RecipesHeaderRed = Color(0xFFFF4D4D)
val RecipesHeaderRedDark = Color(0xFFC72727)

val ShoppingHeaderGreen = Color(0xFF00DD77)
val ShoppingHeaderGreenDark = Color(0xFF00A45E)

val SettingsHeaderPurple = Color(0xFF9333EA)
val SettingsHeaderPurpleDark = Color(0xFF7028C0)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0A7EA4),
    onPrimary = Color.White,
    background = Color.White,
    onBackground = Color(0xFF11181C),
    surface = Color.White,
    onSurface = Color(0xFF11181C),
    surfaceVariant = Color(0xFFF2F3F5),
    onSurfaceVariant = Color(0xFF687076),
)

private val DarkColors = darkColorScheme(
    primary = PlanHeaderBlue,
    onPrimary = Color.White,
    background = Color(0xFF151718),
    onBackground = Color(0xFFECEDEE),
    surface = Color(0xFF151718),
    onSurface = Color(0xFFECEDEE),
    surfaceVariant = Color(0xFF23272A),
    onSurfaceVariant = Color(0xFF9BA1A6),
)

@Composable
fun WeekliesTheme(
    colorScheme: AppColorScheme,
    content: @Composable () -> Unit,
) {
    val dark = when (colorScheme) {
        AppColorScheme.DARK -> true
        AppColorScheme.LIGHT -> false
    } || isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}

/** Header colors follow the active theme like the RN per-tab headers did. */
@Composable
fun headerColorFor(header: HeaderColor): Color = when (header) {
    HeaderColor.PLAN -> if (isSystemInDarkTheme()) PlanHeaderBlueDark else PlanHeaderBlue
    HeaderColor.RECIPES -> if (isSystemInDarkTheme()) RecipesHeaderRedDark else RecipesHeaderRed
    HeaderColor.SHOPPING -> if (isSystemInDarkTheme()) ShoppingHeaderGreenDark else ShoppingHeaderGreen
    HeaderColor.SETTINGS -> if (isSystemInDarkTheme()) SettingsHeaderPurpleDark else SettingsHeaderPurple
}

enum class HeaderColor { PLAN, RECIPES, SHOPPING, SETTINGS }
