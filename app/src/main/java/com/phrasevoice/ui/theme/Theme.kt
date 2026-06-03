package com.phrasevoice.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF2C5E43), // Forest Sage green
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2E8DD),
    onPrimaryContainer = Color(0xFF0A2B18),
    secondary = Color(0xFF6F5E4B), // Warm earth brown
    tertiary = Color(0xFF8B6C5C),
    background = Color(0xFFFAF8F5), // Warm Milk-white
    surface = Color(0xFFFFFFFF),
    onBackground = Color(0xFF262624),
    onSurface = Color(0xFF262624),
    surfaceVariant = Color(0xFFFAF8F5), // Same warm background
    onSurfaceVariant = Color(0xFF4C4B45),
    outline = Color(0xFFE5E2DA), // Warm light gray for border
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF10B981), // Neon Cyan/Teal
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFFD1FAE5),
    secondary = Color(0xFF34D399),
    tertiary = Color(0xFF60A5FA),
    background = Color(0xFF000000), // OLED Pure Black
    surface = Color(0xFF0C0C0C), // Very dark gray for obsidian look
    onBackground = Color(0xFFF3F4F6),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF0C0C0C),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF1E293B), // Sleek border color for OLED black
)

private val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),  // M3 Expressive card style
    large = RoundedCornerShape(28.dp),   // Dialogs, popups
    extraLarge = RoundedCornerShape(36.dp)
)

@Composable
fun PhraseVoiceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        shapes = ExpressiveShapes,
        typography = MaterialTheme.typography,
        content = content,
    )
}
