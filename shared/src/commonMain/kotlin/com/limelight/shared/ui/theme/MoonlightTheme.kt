package com.limelight.shared.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ── Color Palette ──────────────────────────────────────────────────────────
object MoonlightColors {
    val Purple = Color(0xFF7C4DFF)
    val PurpleLight = Color(0xFFB388FF)
    val PurpleDark = Color(0xFF651FFF)
    val Cyan = Color(0xFF00ACC1)
    val CyanLight = Color(0xFF26C6DA)
    val Green = Color(0xFF4CAF50)
    val GreenSurface = Color(0x334CAF50)
    val Red = Color(0xFFEF5350)
    val Amber = Color(0xFFFFB74D)

    val Background = Color(0xFF000000) // Apple Dark Mode uses pure black for backgrounds
    val Surface = Color(0xFF1C1C1E) // Apple System Gray 6
    val SurfaceVariant = Color(0xFF2C2C2E) // Apple System Gray 5
    val SurfaceElevated = Color(0xFF3A3A3C) // Apple System Gray 4
    val OnBackground = Color(0xFFF2F2F7) // Apple System Gray 1
    val OnSurface = Color(0xFFF2F2F7)
    val OnSurfaceVariant = Color(0xFFEBEBF5).copy(alpha = 0.6f) // Apple secondary text
    val Outline = Color(0xFF8E8E93) // Apple System Gray
}

private val DarkColorScheme = darkColorScheme(
    primary = MoonlightColors.Purple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF260059),
    secondary = MoonlightColors.Cyan,
    onSecondary = Color.White,
    background = MoonlightColors.Background,
    onBackground = MoonlightColors.OnBackground,
    surface = MoonlightColors.Surface,
    onSurface = MoonlightColors.OnSurface,
    surfaceVariant = MoonlightColors.SurfaceVariant,
    onSurfaceVariant = MoonlightColors.OnSurfaceVariant,
    outline = MoonlightColors.Outline
)

// ── Typography ─────────────────────────────────────────────────────────────
private val MoonlightTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// ── Theme ──────────────────────────────────────────────────────────────────
@Composable
fun MoonlightTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = MoonlightTypography,
        content = content
    )
}
