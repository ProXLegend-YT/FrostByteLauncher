package com.frostbyte.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.frostbyte.launcher.ui.theme.GlassBorder
import com.frostbyte.launcher.ui.theme.GlassSurface

/**
 * The signature transparent glass card from Section 3 of the PRD:
 * "Transparent glass cards, thin borders, subtle glow, rounded corners,
 * minimal blur, high readability."
 *
 * Deliberately does NOT use Modifier.blur() - real background blur is
 * expensive per-frame on Android and the PRD explicitly says "do not use
 * expensive blur everywhere." Instead this fakes the glass look with a
 * semi-transparent gradient fill + thin border, which is nearly free.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GlassSurface.copy(alpha = 0.55f),
                        GlassSurface.copy(alpha = 0.35f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GlassBorder.copy(alpha = 0.8f),
                        GlassBorder.copy(alpha = 0.25f)
                    )
                ),
                shape = shape
            )
            .padding(contentPadding)
    ) {
        content()
    }
}
