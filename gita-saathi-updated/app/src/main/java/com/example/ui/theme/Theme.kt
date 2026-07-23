package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimaryDark,
    onPrimary = SacredNightBackground,
    primaryContainer = SacredNightContainer,
    onPrimaryContainer = GoldPrimaryDark,
    secondary = AmberSecondaryDark,
    onSecondary = SacredNightBackground,
    tertiary = GoldAccentDark,
    background = SacredNightBackground,
    onBackground = TextLightPrimary,
    surface = SacredNightSurface,
    onSurface = TextLightPrimary,
    surfaceVariant = SacredNightContainer,
    onSurfaceVariant = TextLightSecondary
)

private val LightColorScheme = lightColorScheme(
    primary = SaffronPrimary,
    onPrimary = Color.White,
    primaryContainer = WarmCreamContainer,
    onPrimaryContainer = TextDarkPrimary,
    secondary = SaffronSecondary,
    onSecondary = Color.White,
    tertiary = GoldAccent,
    background = WarmCreamBackground,
    onBackground = TextDarkPrimary,
    surface = WarmCreamSurface,
    onSurface = TextDarkPrimary,
    surfaceVariant = WarmCreamContainer,
    onSurfaceVariant = TextDarkSecondary
)

@Composable
fun GitaSaathiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

