package com.frostbyte.launcher.core.diagnostics

import com.frostbyte.launcher.ui.theme.SpaceQuality

/**
 * Resolves docs/KNOWN_GAPS.md's "no device-tier auto-detection for space
 * rendering quality" item. Pure function of DeviceCapabilities -> SpaceQuality,
 * deliberately kept separate from DeviceCapabilitiesProvider (which needs a
 * real Context) so the actual tiering logic/thresholds are independently
 * unit-testable without Android.
 *
 * Thresholds are conservative/explainable, not tuned against real
 * benchmarking data (no device lab available in this environment) - see
 * docs/KNOWN_GAPS.md for the honest caveat on this.
 */
object SpaceQualityAdvisor {

    fun recommend(capabilities: DeviceCapabilities): SpaceQuality {
        if (capabilities.isLowRamDevice) return SpaceQuality.LOW

        return when {
            capabilities.totalRamMb < 3_000 -> SpaceQuality.LOW
            capabilities.totalRamMb < 6_000 || capabilities.cpuCoreCount < 6 -> SpaceQuality.BALANCED
            capabilities.totalRamMb < 8_000 || capabilities.cpuCoreCount < 8 -> SpaceQuality.HIGH
            else -> SpaceQuality.ULTRA
        }
    }
}
