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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random

data class Star(
    val xFraction: Float,
    val yFraction: Float,
    val radius: Float,
    val baseAlpha: Float,
    val twinkleSpeed: Float, // relative phase multiplier
    val color: Color
)

/**
 * Generates a deterministic-per-session star field. Count scales with quality tier
 * per Section 4 of the PRD (Low = fewer/static, Ultra = most + animated).
 */
fun generateStars(count: Int, seed: Long = 42L): List<Star> {
    val random = Random(seed)
    val palette = listOf(Color(0xFFFFFFFF), Color(0xFFCFE8FF), Color(0xFFEAD9FF))
    return List(count) {
        Star(
            xFraction = random.nextFloat(),
            yFraction = random.nextFloat(),
            radius = random.nextFloat() * 1.6f + 0.4f,
            baseAlpha = random.nextFloat() * 0.6f + 0.35f,
            twinkleSpeed = random.nextFloat() * 0.8f + 0.4f,
            color = palette[random.nextInt(palette.size)]
        )
    }
}

/**
 * Draws twinkling stars. When [animated] is false (Low quality or reduced motion),
 * renders a single static frame with no infinite transition running - this is the
 * cheapest possible path and avoids spinning up animation infra entirely.
 */
@Composable
fun StarField(
    modifier: Modifier = Modifier,
    starCount: Int,
    animated: Boolean
) {
    val stars = remember(starCount) { generateStars(starCount) }

    if (!animated) {
        Canvas(modifier = modifier) {
            drawStars(stars, phase = 0f)
        }
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "starTwinkle")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "starPhase"
    )

    Canvas(modifier = modifier) {
        drawStars(stars, phase)
    }
}

private fun DrawScope.drawStars(stars: List<Star>, phase: Float) {
    val w = size.width
    val h = size.height
    for (star in stars) {
        val twinkle = if (phase == 0f) {
            star.baseAlpha
        } else {
            val t = kotlin.math.sin(phase * star.twinkleSpeed)
            (star.baseAlpha + t * 0.25f).coerceIn(0.1f, 1f)
        }
        drawCircle(
            color = star.color.copy(alpha = twinkle),
            radius = star.radius,
            center = androidx.compose.ui.geometry.Offset(star.xFraction * w, star.yFraction * h)
        )
    }
}
