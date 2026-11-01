package io.github.conorryan.weeklies.ui.theme

import android.app.Activity
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import io.github.conorryan.weeklies.data.model.AppColorScheme

/**
 * "Market" palette: warm paper ground, white cards, deep market green from the
 * launcher icon, tomato for destructive and error states, and a soft violet
 * reserved for AI features (tertiary).
 */
private val LightColors = lightColorScheme(
    primary = Color(0xFF1E6A45),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBDF),
    onPrimaryContainer = Color(0xFF154D32),
    secondaryContainer = Color(0xFFEFEEE7),
    onSecondaryContainer = Color(0xFF18201B),
    tertiaryContainer = Color(0xFFE4E3F1),
    onTertiaryContainer = Color(0xFF3B3270),
    background = Color(0xFFF5F4EE),
    onBackground = Color(0xFF18201B),
    surface = Color.White,
    onSurface = Color(0xFF18201B),
    surfaceVariant = Color(0xFFF5F4EE),
    onSurfaceVariant = Color(0xFF5B655E),
    surfaceContainerLow = Color(0xFFF0F6F1),
    surfaceContainerHighest = Color(0xFFEFEEE7),
    outline = Color(0xFF8A938C),
    outlineVariant = Color(0xFFE2E1D8),
    error = Color(0xFFA33D24),
    onError = Color.White,
    errorContainer = Color(0xFFF6E1D9),
    onErrorContainer = Color(0xFF7A2C18),
    inverseSurface = Color(0xFF18201B),
    inverseOnSurface = Color(0xFFF5F4EE),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8DD3A8),
    onPrimary = Color(0xFF0B2A1A),
    primaryContainer = Color(0xFF244A33),
    onPrimaryContainer = Color(0xFFCDEBD8),
    secondaryContainer = Color(0xFF2A302C),
    onSecondaryContainer = Color(0xFFE8EAE4),
    tertiaryContainer = Color(0xFF34305A),
    onTertiaryContainer = Color(0xFFDAD6FF),
    background = Color(0xFF121614),
    onBackground = Color(0xFFE8EAE4),
    surface = Color(0xFF1B201D),
    onSurface = Color(0xFFE8EAE4),
    surfaceVariant = Color(0xFF121614),
    onSurfaceVariant = Color(0xFFA9B2AB),
    surfaceContainerLow = Color(0xFF1E2A22),
    surfaceContainerHighest = Color(0xFF2A302C),
    outline = Color(0xFF7D867F),
    outlineVariant = Color(0xFF2E3530),
    error = Color(0xFFF2A28C),
    onError = Color(0xFF4A1609),
    errorContainer = Color(0xFF4A2117),
    onErrorContainer = Color(0xFFFFDBD0),
    inverseSurface = Color(0xFFE8EAE4),
    inverseOnSurface = Color(0xFF18201B),
)

private val Base = Typography()

/** Heavier, tighter headings over the default sans; body sizes stay Material's. */
private val WeekliesTypography = Base.copy(
    displaySmall = Base.displaySmall.copy(fontSize = 34.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp),
    headlineMedium = Base.headlineMedium.copy(fontSize = 28.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    headlineSmall = Base.headlineSmall.copy(fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
    titleLarge = Base.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleMedium = Base.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold),
    bodyLarge = Base.bodyLarge.copy(fontSize = 16.sp),
    labelLarge = Base.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
)

/** Small uppercase label above titles and sections ("REST OF THE WEEK"). */
val EyebrowStyle = TextStyle(
    fontSize = 13.sp,
    lineHeight = 16.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.8.sp,
)

private val WeekliesShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Resolved dark/light state of the active theme, as the RN hook useAppColorScheme provided. */
val LocalIsDarkTheme = staticCompositionLocalOf { true }

@Composable
fun WeekliesTheme(
    colorScheme: AppColorScheme,
    content: @Composable () -> Unit,
) {
    val dark = colorScheme == AppColorScheme.DARK
    // enableEdgeToEdge() picks system-bar icon color from the OS theme, but the
    // in-app setting can override it; keep the bars in sync with the resolved theme.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !dark
            insetsController.isAppearanceLightNavigationBars = !dark
        }
    }
    CompositionLocalProvider(LocalIsDarkTheme provides dark) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = WeekliesTypography,
            shapes = WeekliesShapes,
            content = content,
        )
    }
}
