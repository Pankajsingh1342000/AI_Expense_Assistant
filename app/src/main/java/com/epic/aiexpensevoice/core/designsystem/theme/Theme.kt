package com.epic.aiexpensevoice.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = ArchitectPrimary,
    onPrimary = ArchitectSurfaceLowest,
    primaryContainer = ArchitectPrimaryContainer,
    onPrimaryContainer = ArchitectText,
    secondary = ArchitectSecondary,
    onSecondary = ArchitectSurfaceLowest,
    secondaryContainer = ArchitectSecondaryContainer,
    tertiary = ArchitectTertiary,
    background = ArchitectBackground,
    surface = ArchitectSurfaceLowest,
    surfaceVariant = ArchitectSurfaceLow,
    surfaceContainer = ArchitectSurfaceHigh,
    surfaceContainerLow = ArchitectSurfaceLow,
    surfaceContainerLowest = ArchitectSurfaceLowest,
    surfaceContainerHigh = ArchitectSurfaceHigh,
    surfaceContainerHighest = ArchitectSurfaceHighest,
    onBackground = ArchitectText,
    onSurface = ArchitectText,
    onSurfaceVariant = ArchitectTextMuted,
    outline = ArchitectOutline,
    outlineVariant = ArchitectOutlineVariant,
    error = ArchitectError,
)

private val DarkColors = darkColorScheme(
    primary = ArchitectPrimary,
    onPrimary = Color.White,
    primaryContainer = ArchitectPrimaryContainer,
    onPrimaryContainer = Color(0xFF191D30),
    secondary = ArchitectSecondary,
    onSecondary = Color.White,
    secondaryContainer = ArchitectSecondaryContainer,
    tertiary = ArchitectTertiary,
    background = Color(0xFF1E2129), // Flat Dark Background
    surface = Color(0xFF282C37), // Flat Dark Card
    surfaceVariant = Color(0xFF343946),
    surfaceContainer = Color(0xFF282C37),
    surfaceContainerLow = Color(0xFF1E2129),
    surfaceContainerLowest = Color(0xFF1E2129),
    surfaceContainerHigh = Color(0xFF343946),
    surfaceContainerHighest = Color(0xFF3E4352),
    onBackground = Color(0xFFF7F8FB),
    onSurface = Color(0xFFF7F8FB),
    onSurfaceVariant = Color(0xFF8B92A5),
    outline = Color(0xFF4A5060),
    outlineVariant = Color(0xFF343946),
    error = ArchitectError,
)

@Composable
fun AIExpenseVoiceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AIExpenseTypography,
        content = content,
    )
}
