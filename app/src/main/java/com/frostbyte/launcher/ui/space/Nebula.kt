package com.frostbyte.launcher.ui.space

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.frostbyte.launcher.ui.theme.SpaceNebulaBlue
import com.frostbyte.launcher.ui.theme.SpaceNebulaViolet

/**
 * Cheap, static nebula clouds via layered radial gradients. No animation -
 * this layer is the same across all quality tiers since it's near-zero cost
 * (a handful of gradient fills, no per-frame recomputation).
 */
@Composable
fun Nebula(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(SpaceNebulaViolet.copy(alpha = 0.35f), androidx.compose.ui.graphics.Color.Transparent),
                center = Offset(w * 0.18f, h * 0.22f),
                radius = w * 0.65f
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(SpaceNebulaBlue.copy(alpha = 0.4f), androidx.compose.ui.graphics.Color.Transparent),
                center = Offset(w * 0.85f, h * 0.75f),
                radius = w * 0.7f
            )
        )
    }
}
