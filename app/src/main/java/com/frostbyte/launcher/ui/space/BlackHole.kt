package com.frostbyte.launcher.ui.space

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.frostbyte.launcher.ui.theme.AccretionGold
import com.frostbyte.launcher.ui.theme.AccretionOrange
import com.frostbyte.launcher.ui.theme.AccretionPink
import com.frostbyte.launcher.ui.theme.IceBlue
import com.frostbyte.launcher.ui.theme.SpaceQuality

/**
 * The black hole + accretion disk, per Section 4 of the PRD.
 *
 * - LOW: single static frame, no rotation - cheapest possible render.
 * - BALANCED: disk rotates slowly, no extra particle layer.
 * - HIGH/ULTRA: rotation + a subtle secondary glow ring for depth.
 *
 * [animated] is a separate flag from quality so reduced-motion accessibility
 * (Section 34) can force a static frame regardless of device quality tier.
 */
@Composable
fun BlackHole(
    modifier: Modifier = Modifier,
    quality: SpaceQuality,
    animated: Boolean
) {
    val rotation = if (animated) {
        val infiniteTransition = rememberInfiniteTransition(label = "diskRotation")
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 24000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "diskAngle"
        )
    } else {
        null
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w * 0.62f, h * 0.32f)
        val eventHorizonRadius = w * 0.20f
        val diskOuterRadius = w * 0.42f

        val angle = rotation?.value ?: 0f

        rotate(degrees = angle, pivot = center) {
            // Accretion disk glow ring (outer)
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        AccretionGold,
                        AccretionOrange,
                        AccretionPink,
                        IceBlue,
                        AccretionGold
                    ),
                    center = center
                ),
                radius = diskOuterRadius,
                center = center,
                alpha = 0.55f
            )

            if (quality == SpaceQuality.HIGH || quality == SpaceQuality.ULTRA) {
                // Secondary thinner ring for extra depth on higher-end tiers
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(IceBlue, AccretionPink, AccretionGold, IceBlue),
                        center = center
                    ),
                    radius = diskOuterRadius * 1.18f,
                    center = center,
                    alpha = 0.22f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.01f)
                )
            }
        }

        // Soft outer glow behind the disk (not rotated, just a static gradient wash)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(AccretionOrange.copy(alpha = 0.18f), Color.Transparent),
                center = center,
                radius = diskOuterRadius * 1.6f
            ),
            radius = diskOuterRadius * 1.6f,
            center = center
        )

        // Event horizon - pure black core, drawn last so it always occludes the disk
        drawCircle(
            color = Color.Black,
            radius = eventHorizonRadius,
            center = center
        )

        // Thin bright rim right at the horizon edge for contrast
        drawCircle(
            color = IceBlue.copy(alpha = 0.6f),
            radius = eventHorizonRadius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.004f)
        )
    }
}
