package com.limelight.shared.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.limelight.shared.ui.theme.MoonlightColors

@Composable
fun AetherisScreen(
    modifier: Modifier = Modifier,
    primaryGlowAlignment: Alignment = Alignment.TopStart,
    secondaryGlowAlignment: Alignment = Alignment.BottomEnd,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MoonlightColors.Background)
    ) {
        AetherisGlow(
            modifier = Modifier
                .align(primaryGlowAlignment)
                .offset(x = (-120).dp, y = (-140).dp),
            color = MoonlightColors.PrimaryContainer
        )
        AetherisGlow(
            modifier = Modifier
                .align(secondaryGlowAlignment)
                .offset(x = 140.dp, y = 120.dp),
            color = MoonlightColors.SecondaryContainer
        )
        content()
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.14f),
                        Color.White.copy(alpha = 0.05f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            ),
        color = Color.Transparent,
        shape = RoundedCornerShape(cornerRadius),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.07f),
                            Color.White.copy(alpha = 0.025f)
                        )
                    )
                )
                .padding(contentPadding)
        ) {
            content()
        }
    }
}

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        color = MoonlightColors.GlassBackground
    ) { content() }
}

@Composable
fun AetherisGlow(
    modifier: Modifier = Modifier,
    color: Color = MoonlightColors.Primary,
    size: Dp = 420.dp,
    alpha: Float = 0.16f
) {
    Box(
        modifier = modifier
            .size(size)
            .blur(110.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), Color.Transparent)
                ),
                CircleShape
            )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeHubTopBar(
    title: String = "Home Hub",
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Hub, contentDescription = null, tint = MoonlightColors.PrimaryFixedDim, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(12.dp))
                Text(title, color = MoonlightColors.OnSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = MoonlightColors.OnSurface)
                }
            }
        },
        actions = {
            actions()
            Box(
                modifier = Modifier
                    .padding(end = 12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MoonlightColors.SurfaceVariant)
                    .border(1.dp, Color.White.copy(alpha = 0.10f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = "Perfil", tint = MoonlightColors.OnSurfaceVariant, modifier = Modifier.size(22.dp))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MoonlightColors.Surface.copy(alpha = 0.62f),
            titleContentColor = MoonlightColors.OnSurface,
            actionIconContentColor = MoonlightColors.OnSurface
        )
    )
}

@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    leadingDot: Boolean = true
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.30f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingDot) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
fun PrimaryGlassButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MoonlightColors.PrimaryContainer,
            contentColor = Color.White,
            disabledContainerColor = MoonlightColors.SurfaceContainerHighest,
            disabledContentColor = MoonlightColors.OnSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
    }
}

@Composable
fun ErrorGlassButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        enabled = enabled,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, MoonlightColors.Error.copy(alpha = 0.28f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MoonlightColors.Error.copy(alpha = 0.10f),
            contentColor = MoonlightColors.Error
        ),
        contentPadding = PaddingValues(horizontal = 24.dp)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
    }
}
