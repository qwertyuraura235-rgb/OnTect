package com.park.reagentkeeper.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF064E52),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC8F4F0),
    onPrimaryContainer = Color(0xFF062525),
    secondary = Color(0xFF9B5E16),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFE0B5),
    onSecondaryContainer = Color(0xFF2D1800),
    tertiary = Color(0xFF356846),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD7F1DE),
    onTertiaryContainer = Color(0xFF0A2414),
    background = Color(0xFFF7F3E8),
    onBackground = Color(0xFF141B19),
    surface = Color(0xFFFFFCF6),
    onSurface = Color(0xFF18201E),
    surfaceVariant = Color(0xFFE3EBE7),
    onSurfaceVariant = Color(0xFF46524F),
    outline = Color(0xFF75827E),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9FE4DF),
    onPrimary = Color(0xFF00363B),
    primaryContainer = Color(0xFF0D5155),
    onPrimaryContainer = Color(0xFFC8F4F0),
    secondary = Color(0xFFFFC979),
    onSecondary = Color(0xFF472A00),
    secondaryContainer = Color(0xFF6D4107),
    onSecondaryContainer = Color(0xFFFFE0B5),
    tertiary = Color(0xFFA8D3B4),
    onTertiary = Color(0xFF10351D),
    tertiaryContainer = Color(0xFF214D30),
    onTertiaryContainer = Color(0xFFD7F1DE),
    background = Color(0xFF0D1312),
    onBackground = Color(0xFFE3E8E5),
    surface = Color(0xFF151B1A),
    onSurface = Color(0xFFE3E8E5),
    surfaceVariant = Color(0xFF3F4947),
    onSurfaceVariant = Color(0xFFBEC9C6),
    outline = Color(0xFF889390),
)

private val OnTectTypography = Typography(
    displaySmall = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 42.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1.2).sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp,
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 21.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
)

private val OnTectShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(34.dp),
)

@Composable
fun ReagentKeeperTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = OnTectTypography,
        shapes = OnTectShapes,
        content = content,
    )
}
