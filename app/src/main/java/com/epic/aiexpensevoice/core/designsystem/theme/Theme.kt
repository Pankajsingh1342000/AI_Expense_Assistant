package com.epic.aiexpensevoice.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Mint500,
    onPrimary = Navy900,
    secondary = Azure400,
    tertiary = Amber400,
    background = Cloud100,
    surface = androidx.compose.ui.graphics.Color.White,
    onBackground = Navy900,
    onSurface = Navy900,
    error = Coral400,
)

private val DarkColors = darkColorScheme(
    primary = Mint300,
    onPrimary = Navy900,
    secondary = Azure400,
    tertiary = Amber400,
    background = Navy900,
    surface = Navy700,
    onBackground = Cloud100,
    onSurface = Cloud100,
    error = Coral400,
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
