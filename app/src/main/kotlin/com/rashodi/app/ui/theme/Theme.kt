package com.rashodi.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Дополнительные семантические цвета вне стандартной ColorScheme. */
data class AppExtraColors(
    val income: Color,
    val expense: Color,
    val surfaceHigh: Color,
    val chart: List<Color>,
)

val LocalAppColors = staticCompositionLocalOf {
    AppExtraColors(IncomeMint, ExpenseRose, DarkSurfaceHigh, ChartPaletteDark)
}

private val DarkScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    error = ExpenseRose,
    onError = Color(0xFF2A0A0E),
)

private val LightScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline,
    error = ExpenseRoseDark,
    onError = Color(0xFFFFFFFF),
)

@Composable
fun RashodiTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkScheme else LightScheme
    val extra = if (darkTheme) {
        AppExtraColors(IncomeMint, ExpenseRose, DarkSurfaceHigh, ChartPaletteDark)
    } else {
        AppExtraColors(IncomeMintDark, ExpenseRoseDark, LightSurfaceHigh, ChartPaletteLight)
    }
    CompositionLocalProvider(LocalAppColors provides extra) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
