package com.frostbyte.launcher.ui.space

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.frostbyte.launcher.ui.theme.LocalReducedMotion
import com.frostbyte.launcher.ui.theme.LocalSpaceQuality
import com.frostbyte.launcher.ui.theme.SpaceQuality
import com.frostbyte.launcher.ui.theme.SpaceVoid

/**
 * SpaceRenderer - Section 4 of the PRD.
 *
 * Composes: StarField -> Nebula -> BlackHole -> CosmicParticles, back to front.
 * This is the ONE place that decides, per quality tier, which layers animate
 * and how many stars/particles to draw. Screens should just place this once
 * behind their content and never touch the individual layers directly.
 *
 * Visibility handling: observes the lifecycle and stops all animation when the
 * app is backgrounded (ON_STOP), matching "pause background animation when the
 * app is not visible" from the PRD. This isn't just cosmetic - infinite Compose
 * animations keep recomposing even off-screen unless something tells them to stop.
 */
@Composable
fun SpaceRenderer(modifier: Modifier = Modifier) {
    val quality = LocalSpaceQuality.current
    val reducedMotion = LocalReducedMotion.current

    var isVisible by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> isVisible = false
                Lifecycle.Event.ON_START -> isVisible = true
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val animationsEnabled = isVisible && !reducedMotion

    val starCount = when (quality) {
        SpaceQuality.LOW -> 60
        SpaceQuality.BALANCED -> 140
        SpaceQuality.HIGH -> 220
        SpaceQuality.ULTRA -> 320
    }

    // Low quality never animates the disk either, per "Static/pre-rendered
    // black-hole artwork" in the PRD - even if the app is visible and motion
    // is allowed, Low intentionally stays still to save cost.
    val blackHoleAnimated = animationsEnabled && quality != SpaceQuality.LOW
    val starsAnimated = animationsEnabled && quality != SpaceQuality.LOW

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpaceVoid)
    ) {
        Nebula(modifier = Modifier.fillMaxSize())
        StarField(
            modifier = Modifier.fillMaxSize(),
            starCount = starCount,
            animated = starsAnimated
        )
        BlackHole(
            modifier = Modifier.fillMaxSize(),
            quality = quality,
            animated = blackHoleAnimated
        )
        if (animationsEnabled && (quality == SpaceQuality.HIGH || quality == SpaceQuality.ULTRA)) {
            CosmicParticles(modifier = Modifier.fillMaxSize())
        }
    }
}
