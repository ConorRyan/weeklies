package com.conorryan.weeklies.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
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

/** Resolved dark/light state of the active theme, as the RN hook useAppColorScheme provided. */
val LocalIsDarkTheme = staticCompositionLocalOf { true }

@Composable
fun WeekliesTheme(
    colorScheme: AppColorScheme,
    content: @Composable () -> Unit,
) {
    val dark = colorScheme == AppColorScheme.DARK
    CompositionLocalProvider(LocalIsDarkTheme provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            content = content,
        )
    }
}

/** Header colors follow the active theme like the RN per-tab headers did. */
@Composable
fun headerColorFor(header: HeaderColor): Color = when (header) {
    HeaderColor.PLAN -> if (LocalIsDarkTheme.current) PlanHeaderBlueDark else PlanHeaderBlue
    HeaderColor.RECIPES -> if (LocalIsDarkTheme.current) RecipesHeaderRedDark else RecipesHeaderRed
    HeaderColor.SHOPPING -> if (LocalIsDarkTheme.current) ShoppingHeaderGreenDark else ShoppingHeaderGreen
    HeaderColor.SETTINGS -> if (LocalIsDarkTheme.current) SettingsHeaderPurpleDark else SettingsHeaderPurple
}

enum class HeaderColor { PLAN, RECIPES, SHOPPING, SETTINGS }
