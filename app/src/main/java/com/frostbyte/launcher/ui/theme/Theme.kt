package com.frostbyte.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * FrostByte is a dark, space-themed UI by design (Section 3 of PRD) - there is
 * intentionally no light theme variant, since the cosmic identity IS the brand.
 */
private val FrostByteColorScheme = darkColorScheme(
    primary = IceBlue,
    onPrimary = SpaceVoid,
    secondary = CosmicViolet,
    onSecondary = SpaceVoid,
    tertiary = AccretionOrange,
    background = SpaceVoid,
    onBackground = TextPrimary,
    surface = GlassSurface,
    onSurface = TextPrimary,
    surfaceVariant = GlassSurface,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    error = StatusRed,
    onError = TextPrimary
)

/**
 * Global reduced-motion flag (Section 34: Accessibility).
 * When true, the black-hole renderer and large transitions should disable
 * animation. Defaults to following the system "remove animations" setting;
 * screens read this via LocalReducedMotion.current.
 */
val LocalReducedMotion = staticCompositionLocalOf { false }

/**
 * Global render quality tier for the space background (Section 4: Black-Hole Renderer).
 * Screens/components read this instead of each independently detecting device tier.
 */
enum class SpaceQuality { LOW, BALANCED, HIGH, ULTRA }

val LocalSpaceQuality = compositionLocalOf { SpaceQuality.BALANCED }

@Composable
fun FrostByteTheme(
    reducedMotion: Boolean = false,
    spaceQuality: SpaceQuality = SpaceQuality.BALANCED,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalReducedMotion provides reducedMotion,
        LocalSpaceQuality provides spaceQuality
    ) {
        MaterialTheme(
            colorScheme = FrostByteColorScheme,
            typography = FrostByteTypography,
            content = content
        )
    }
}
