package com.frostbyte.launcher.core.storage.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.frostbyte.launcher.ui.theme.SpaceQuality
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "frostbyte_settings")

/**
 * App-wide settings, Section 27 (Privacy: telemetry OFF by default) and
 * Section 34 (Accessibility: reduced motion override) of the PRD.
 *
 * These are DataStore, not Room, because they're a handful of scalar
 * key-value app preferences, not relational records - Room would be the
 * wrong tool here (Section 28 of the PRD lists both DataStore and Room as
 * intended for different jobs).
 */
data class AppSettings(
    val reducedMotionOverride: Boolean? = null, // null = follow system setting
    val spaceQualityOverride: SpaceQuality? = null, // null = auto-detect from device capabilities (SpaceQualityAdvisor)
    val telemetryEnabled: Boolean = false, // OFF by default, per Section 27
    val crashReportsEnabled: Boolean = false // must be explicit opt-in, per Section 27
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val REDUCED_MOTION_SET = booleanPreferencesKey("reduced_motion_set")
        val REDUCED_MOTION_VALUE = booleanPreferencesKey("reduced_motion_value")
        val SPACE_QUALITY_OVERRIDE = stringPreferencesKey("space_quality_override") // absent = auto-detect
        val TELEMETRY_ENABLED = booleanPreferencesKey("telemetry_enabled")
        val CRASH_REPORTS_ENABLED = booleanPreferencesKey("crash_reports_enabled")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        val reducedMotionOverride = if (prefs[Keys.REDUCED_MOTION_SET] == true) {
            prefs[Keys.REDUCED_MOTION_VALUE] ?: false
        } else {
            null
        }
        AppSettings(
            reducedMotionOverride = reducedMotionOverride,
            spaceQualityOverride = prefs[Keys.SPACE_QUALITY_OVERRIDE]?.let {
                runCatching { SpaceQuality.valueOf(it) }.getOrNull()
            },
            telemetryEnabled = prefs[Keys.TELEMETRY_ENABLED] ?: false,
            crashReportsEnabled = prefs[Keys.CRASH_REPORTS_ENABLED] ?: false
        )
    }

    suspend fun setReducedMotionOverride(enabled: Boolean?) {
        context.settingsDataStore.edit { prefs ->
            if (enabled == null) {
                prefs[Keys.REDUCED_MOTION_SET] = false
            } else {
                prefs[Keys.REDUCED_MOTION_SET] = true
                prefs[Keys.REDUCED_MOTION_VALUE] = enabled
            }
        }
    }

    /** Pass null to clear the manual override and go back to auto-detection. */
    suspend fun setSpaceQualityOverride(quality: SpaceQuality?) {
        context.settingsDataStore.edit { prefs ->
            if (quality == null) {
                prefs.remove(Keys.SPACE_QUALITY_OVERRIDE)
            } else {
                prefs[Keys.SPACE_QUALITY_OVERRIDE] = quality.name
            }
        }
    }

    suspend fun setTelemetryEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[Keys.TELEMETRY_ENABLED] = enabled }
    }

    suspend fun setCrashReportsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs -> prefs[Keys.CRASH_REPORTS_ENABLED] = enabled }
    }
}
