package com.frostbyte.launcher.ui.space

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.random.Random

private data class Particle(
    val xFraction: Float,
    val startYFraction: Float,
    val speed: Float,
    val radius: Float,
    val alpha: Float
)

/**
 * Slow-drifting dust motes. Per Section 4, this layer only renders at
 * HIGH/ULTRA quality - callers are expected to gate this out entirely at
 * LOW/BALANCED rather than rendering it invisibly, to actually save the cost.
 */
@Composable
fun CosmicParticles(
    modifier: Modifier = Modifier,
    count: Int = 24
) {
    val particles = remember(count) {
        val random = Random(7L)
        List(count) {
            Particle(
                xFraction = random.nextFloat(),
                startYFraction = random.nextFloat(),
                speed = random.nextFloat() * 0.6f + 0.2f,
                radius = random.nextFloat() * 1.2f + 0.3f,
                alpha = random.nextFloat() * 0.4f + 0.15f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particleDrift")
    val drift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "driftProgress"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        for (p in particles) {
            val y = (p.startYFraction + drift * p.speed) % 1f
            drawCircle(
                color = Color.White.copy(alpha = p.alpha),
                radius = p.radius,
                center = Offset(p.xFraction * w, y * h)
            )
        }
    }
}
