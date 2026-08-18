package com.frostbyte.launcher

import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.frostbyte.launcher.core.diagnostics.SpaceQualityAdvisor
import com.frostbyte.launcher.core.storage.datastore.AppSettings
import com.frostbyte.launcher.ui.navigation.FrostByteNavHost
import com.frostbyte.launcher.ui.navigation.FrostByteNavScaffold
import com.frostbyte.launcher.ui.theme.FrostByteTheme
import com.frostbyte.launcher.ui.theme.SpaceQuality

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Section 34: respect the system-level "remove animations" accessibility
        // setting as the default reduced-motion signal, unless the user has set
        // an explicit override in Settings (Phase 2), which takes precedence.
        val systemReducedMotion = try {
            Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        } catch (e: Settings.SettingNotFoundException) {
            false
        }

        val container = (application as FrostByteApplication).container

        // Resolves docs/KNOWN_GAPS.md's "no device-tier auto-detection for
        // space rendering quality" item (Phase 9). Computed once at startup,
        // not per-frame - device capabilities don't change mid-session.
        val recommendedQuality = SpaceQualityAdvisor.recommend(container.deviceCapabilitiesProvider.detect())

        setContent {
            val appSettings by container.settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings()
            )
            val reducedMotion = appSettings.reducedMotionOverride ?: systemReducedMotion
            val spaceQuality = appSettings.spaceQualityOverride ?: recommendedQuality

            FrostByteRoot(reducedMotion = reducedMotion, spaceQuality = spaceQuality)
        }
    }
}

/**
 * Space quality is now genuinely auto-detected from real device capabilities
 * (SpaceQualityAdvisor, Phase 9) when the user hasn't manually overridden it
 * in Settings (Phase 2) - resolves the gap previously tracked in
 * docs/KNOWN_GAPS.md.
 */
@Composable
private fun FrostByteRoot(
    reducedMotion: Boolean,
    spaceQuality: SpaceQuality
) {
    FrostByteTheme(reducedMotion = reducedMotion, spaceQuality = spaceQuality) {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            val windowWidthDp = LocalConfiguration.current.screenWidthDp

            Box(modifier = Modifier.fillMaxSize()) {
                FrostByteNavScaffold(
                    navController = navController,
                    windowWidthDp = windowWidthDp
                ) { modifier ->
                    FrostByteNavHost(navController = navController, modifier = modifier)
                }
            }
        }
    }
}
