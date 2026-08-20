package com.frostbyte.launcher.ui.space

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.frostbyte.launcher.R

/**
 * SpaceRenderer - Section 4 of the PRD.
 *
 * Draws the static cosmic black-hole background artwork (res/drawable/bg_space_blackhole).
 * Previously this composed four animated layers (StarField, Nebula, BlackHole,
 * CosmicParticles); that was replaced with a single static image per an
 * explicit product decision to trade the animation for a fixed piece of art,
 * lower battery/CPU cost, and a smaller amount of moving-parts code to
 * maintain. The quality/reducedMotion signature is kept as-is so screens and
 * the Settings screen's space-quality picker don't need to change - a static
 * image just doesn't need to read them to decide anything.
 */
@Composable
fun SpaceRenderer(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg_space_blackhole),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

