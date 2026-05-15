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
    val Background = Color(0xFF131315)
    val Surface = Color(0xFF131315)
    val SurfaceVariant = Color(0xFF1C1C1E)
    val OnBackground = Color(0xFFe4e2e4)
    val OnSurface = Color(0xFFe4e2e4)
    val OnSurfaceVariant = Color(0xFFc1c6d7)
    val Primary = Color(0xFFadc6ff)
    val PrimaryContainer = Color(0xFF4b8eff)
    val OnPrimaryContainer = Color(0xFF00285c)
    val Secondary = Color(0xFF53e16f)
    val SecondaryContainer = Color(0xFF05b046)
    val Tertiary = Color(0xFFe8b3ff)
    val TertiaryContainer = Color(0xFFc567f4)
    val Outline = Color(0xFF8b90a0)
    val Error = Color(0xFFffb4ab)
    val ErrorContainer = Color(0xFF93000a)
    val OnErrorContainer = Color(0xFFffdad6)
}

private val DarkColorScheme = darkColorScheme(
    primary = MoonlightColors.Primary,
    primaryContainer = MoonlightColors.PrimaryContainer,
    onPrimaryContainer = MoonlightColors.OnPrimaryContainer,
    secondary = MoonlightColors.Secondary,
    secondaryContainer = MoonlightColors.SecondaryContainer,
    tertiary = MoonlightColors.Tertiary,
    tertiaryContainer = MoonlightColors.TertiaryContainer,
    background = MoonlightColors.Background,
    onBackground = MoonlightColors.OnBackground,
    surface = MoonlightColors.Surface,
    onSurface = MoonlightColors.OnSurface,
    surfaceVariant = MoonlightColors.SurfaceVariant,
    onSurfaceVariant = MoonlightColors.OnSurfaceVariant,
    outline = MoonlightColors.Outline,
    error = MoonlightColors.Error,
    errorContainer = MoonlightColors.ErrorContainer,
    onErrorContainer = MoonlightColors.OnErrorContainer
)

// ── Typography ─────────────────────────────────────────────────────────────
private val MoonlightTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 41.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.35.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.4).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.2).sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
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
