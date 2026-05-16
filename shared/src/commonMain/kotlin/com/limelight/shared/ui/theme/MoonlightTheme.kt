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
    val Background = Color(0xFF10131B)
    val Surface = Color(0xFF1A1C1E)
    val SurfaceVariant = Color(0xFF1C1C1E)
    val OnBackground = Color(0xFFE0E2ED)
    val OnSurface = Color(0xFFE0E2ED)
    val OnSurfaceVariant = Color(0xFFC1C6D7)
    val Primary = Color(0xFFADC6FF)
    val PrimaryContainer = Color(0xFF4B8EFF)
    val OnPrimaryContainer = Color(0xFF00285C)
    val Secondary = Color(0xFF53E16F)
    val SecondaryContainer = Color(0xFF05B046)
    val Tertiary = Color(0xFFE8B3FF)
    val TertiaryContainer = Color(0xFFC567F4)
    val Outline = Color(0xFF8B90A0)
    val Error = Color(0xFFFFB4AB)
    val ErrorContainer = Color(0xFF93000A)
    val OnErrorContainer = Color(0xFFFFDAD6)
    
    // Aetheris Specials
    val GlowPrimary = Color(0x26ADC6FF) // 15% opacity
    val GlassBackground = Color(0x6610131B) // 40% opacity
    val GlassBorder = Color(0x1AFFFFFF) // 10% opacity
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
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
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
