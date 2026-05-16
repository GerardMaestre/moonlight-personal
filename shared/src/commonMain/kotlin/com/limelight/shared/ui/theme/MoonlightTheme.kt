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
    val Error = Color(0xFFFFB4AB)
    val SecondaryFixedDim = Color(0xFFD1BCFF)
    val TertiaryFixed = Color(0xFF7DF4FF)
    val OnErrorContainer = Color(0xFFFFDAD6)
    val InversePrimary = Color(0xFF005BC1)
    val OnBackground = Color(0xFFE0E2ED)
    val Surface = Color(0xFF10131B)
    val OutlineVariant = Color(0xFF414755)
    val Primary = Color(0xFFADC6FF)
    val TertiaryFixedDim = Color(0xFF00DBE9)
    val OnError = Color(0xFF690005)
    val Tertiary = Color(0xFF00DBE9)
    val InverseSurface = Color(0xFFE0E2ED)
    val OnSecondary = Color(0xFF3C0090)
    val SurfaceContainerLowest = Color(0xFF0B0E16)
    val SurfaceVariant = Color(0xFF31353D)
    val PrimaryContainer = Color(0xFF4B8EFF)
    val OnSecondaryContainer = Color(0xFFDDCDFF)
    val OnTertiaryContainer = Color(0xFF002F33)
    val OnTertiary = Color(0xFF00363A)
    val InverseOnSurface = Color(0xFF2D3039)
    val OnPrimary = Color(0xFF002E69)
    val SecondaryContainer = Color(0xFF7000FF)
    val OnPrimaryContainer = Color(0xFF00285C)
    val SurfaceBright = Color(0xFF363942)
    val PrimaryFixed = Color(0xFFD8E2FF)
    val SurfaceDim = Color(0xFF10131B)
    val SecondaryFixed = Color(0xFFE9DDFF)
    val PrimaryFixedDim = Color(0xFFADC6FF)
    val SurfaceTint = Color(0xFFADC6FF)
    val Background = Color(0xFF10131B)
    val OnSurfaceVariant = Color(0xFFC1C6D7)
    val OnPrimaryFixed = Color(0xFF001A41)
    val TertiaryContainer = Color(0xFF00A0AA)
    val SurfaceContainerHigh = Color(0xFF272A32)
    val SurfaceContainer = Color(0xFF1C2028)
    val Outline = Color(0xFF8B90A0)
    val SurfaceContainerHighest = Color(0xFF31353D)
    val ErrorContainer = Color(0xFF93000A)
    val SurfaceContainerLow = Color(0xFF181C23)
    val OnSurface = Color(0xFFE0E2ED)
    val Secondary = Color(0xFFD1BCFF)

    val GlowPrimary = Color(0x26ADC6FF)
    val GlowSecondary = Color(0x1A7000FF)
    val GlowTertiary = Color(0x2600DBE9)
    val GlassBackground = Color(0x6610131B)
    val GlassBorder = Color(0x1AFFFFFF)
    val GlassHighlight = Color(0x0FFFFFFF)
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
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
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
