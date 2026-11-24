package com.dreamy.mobile.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// The web app has a single light theme, so the Android app does too.
private val DreamyColors = lightColorScheme(
    primary = Cyan,
    onPrimary = White,
    primaryContainer = Light,
    onPrimaryContainer = Navy,
    secondary = Navy,
    onSecondary = White,
    secondaryContainer = Light,
    onSecondaryContainer = Navy,
    tertiary = Coral,
    onTertiary = White,
    background = Pale,
    onBackground = Navy,
    surface = White,
    onSurface = Navy,
    surfaceVariant = Pale,
    onSurfaceVariant = NavyMuted,
    surfaceContainer = White,
    surfaceContainerHigh = White,
    surfaceContainerHighest = Pale,
    surfaceContainerLow = White,
    outline = Light,
    outlineVariant = Light,
    error = Coral,
    onError = White,
)

private val DreamyShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(14.dp), // .field
    large = RoundedCornerShape(24.dp), // .card
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun DreamyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DreamyColors,
        typography = DreamyTypography,
        shapes = DreamyShapes,
        content = content,
    )
}
