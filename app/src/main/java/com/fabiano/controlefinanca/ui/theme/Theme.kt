package com.fabiano.controlefinanca.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    secondary = PinkAccent,
    tertiary = GreenAccent,
    background = NightBackground,
    surface = NightSurface,
    surfaceVariant = NightSurfaceVariant,
    onPrimary = NightBackground,
    onSecondary = OnDark,
    onTertiary = NightBackground,
    onBackground = OnDark,
    onSurface = OnDark
)

@Composable
fun ControleFinancaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
