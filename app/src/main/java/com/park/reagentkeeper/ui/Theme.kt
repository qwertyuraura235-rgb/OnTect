package com.park.reagentkeeper.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F5C63),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFAEEFF2),
    onPrimaryContainer = Color(0xFF002022),
    secondary = Color(0xFF855A00),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDEA6),
    onSecondaryContainer = Color(0xFF2A1800),
    tertiary = Color(0xFF9A2C2C),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF3F8F7),
    onBackground = Color(0xFF121715),
    surface = Color(0xFFFFFBF5),
    onSurface = Color(0xFF1A1C1B),
    surfaceVariant = Color(0xFFDCE5E3),
    onSurfaceVariant = Color(0xFF3F4947),
    outline = Color(0xFF6F7977),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF92D9DC),
    onPrimary = Color(0xFF00363B),
    primaryContainer = Color(0xFF004F55),
    onPrimaryContainer = Color(0xFFAEEFF2),
    secondary = Color(0xFFFFC95C),
    onSecondary = Color(0xFF472A00),
    secondaryContainer = Color(0xFF654000),
    onSecondaryContainer = Color(0xFFFFDEA6),
    tertiary = Color(0xFFFFB3AF),
    onTertiary = Color(0xFF601313),
    background = Color(0xFF0F1413),
    onBackground = Color(0xFFE0E4E2),
    surface = Color(0xFF171C1B),
    onSurface = Color(0xFFE0E4E2),
    surfaceVariant = Color(0xFF3F4947),
    onSurfaceVariant = Color(0xFFBEC9C6),
    outline = Color(0xFF889390),
)

@Composable
fun ReagentKeeperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography(),
        content = content,
    )
}
