package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    onPrimary = OnPrimaryPurple,
    secondary = AccentAmber,
    onSecondary = Color.Black,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = CardBackground,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = TextSecondary,
    error = AccentRed,
    onError = Color.White
)

@Composable
fun LeetCodeCompanionTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
