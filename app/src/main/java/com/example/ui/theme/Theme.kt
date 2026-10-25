package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = PanuGold,
    background = PanuDarkBackground,
    surface = PanuSurfaceDark,
    onPrimary = Color.Black,
    onBackground = PanuTextWhite,
    onSurface = PanuTextWhite
)

class PanuColors(
    val materialScheme: ColorScheme,
    val champagne: Color = PanuGold,
    val champagneLight: Color = Color(0xFFF3C775),
    val champagneDark: Color = Color(0xFFB57E1E),
    val champagneSubtle: Color = Color(0x33E5A93C),
    val emerald: Color = PanuEmerald,
    val emeraldSubtle: Color = Color(0x3310B981),
    val surfaceElevated: Color = Color(0xFF1E2230),
    val surfaceBorder: Color = Color(0xFF2E3446),
    val textPrimary: Color = PanuTextWhite,
    val textSecondary: Color = PanuGray,
    val textMuted: Color = Color(0xFF6B7280),
    val isDark: Boolean = true
) {
    val background: Color get() = materialScheme.background
    val surface: Color get() = materialScheme.surface
    val error: Color get() = materialScheme.error
    val primary: Color get() = materialScheme.primary
    val onPrimary: Color get() = materialScheme.onPrimary
    val onBackground: Color get() = materialScheme.onBackground
    val onSurface: Color get() = materialScheme.onSurface
    val outline: Color get() = materialScheme.outline
    val secondary: Color get() = materialScheme.secondary
}

object PanuTheme {
    val colors: PanuColors
        @Composable
        @ReadOnlyComposable
        get() = PanuColors(materialScheme = MaterialTheme.colorScheme)
}

val ColorScheme.champagne: Color get() = PanuGold
val ColorScheme.champagneLight: Color get() = Color(0xFFF3C775)
val ColorScheme.champagneDark: Color get() = Color(0xFFB57E1E)
val ColorScheme.champagneSubtle: Color get() = Color(0x33E5A93C)
val ColorScheme.emerald: Color get() = PanuEmerald
val ColorScheme.emeraldSubtle: Color get() = Color(0x3310B981)
val ColorScheme.surfaceElevated: Color get() = Color(0xFF1E2230)
val ColorScheme.surfaceBorder: Color get() = Color(0xFF2E3446)
val ColorScheme.textPrimary: Color get() = PanuTextWhite
val ColorScheme.textSecondary: Color get() = PanuGray
val ColorScheme.textMuted: Color get() = Color(0xFF6B7280)
val ColorScheme.isDark: Boolean get() = true

@Composable
fun PanuTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
