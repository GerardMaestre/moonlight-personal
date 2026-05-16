package com.limelight.shared.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.limelight.shared.ui.theme.MoonlightColors

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        // Background Glass
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(cornerRadius))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    shape = RoundedCornerShape(cornerRadius)
                ),
            color = MoonlightColors.GlassBackground,
            shape = RoundedCornerShape(cornerRadius)
        ) {
            content()
        }
    }
}

@Composable
fun AetherisGlow(
    modifier: Modifier = Modifier,
    color: Color = MoonlightColors.Primary
) {
    Box(
        modifier = modifier
            .size(300.dp)
            .blur(80.dp)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        color.copy(alpha = 0.15f),
                        Color.Transparent
                    )
                )
            )
    )
}
