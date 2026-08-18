package com.frostbyte.launcher.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostbyte.launcher.core.diagnostics.DeviceCapabilitiesProvider
import com.frostbyte.launcher.core.diagnostics.SpaceQualityAdvisor
import com.frostbyte.launcher.core.storage.datastore.AppSettings
import com.frostbyte.launcher.core.storage.datastore.SettingsRepository
import com.frostbyte.launcher.ui.theme.SpaceQuality
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val appSettings: AppSettings = AppSettings(),
    val recommendedSpaceQuality: SpaceQuality = SpaceQuality.BALANCED
) {
    /** The quality actually in effect right now - the user's manual override if set, otherwise the device-recommended tier. */
    val effectiveSpaceQuality: SpaceQuality
        get() = appSettings.spaceQualityOverride ?: recommendedSpaceQuality
}

class SettingsViewModel(
    private val repository: SettingsRepository,
    deviceCapabilitiesProvider: DeviceCapabilitiesProvider
) : ViewModel() {

    // Device capabilities don't change during a session, so this is computed
    // once rather than re-queried on every recomposition.
    private val recommendedQuality = SpaceQualityAdvisor.recommend(deviceCapabilitiesProvider.detect())
    private val recommendedQualityFlow = MutableStateFlow(recommendedQuality)

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.settings,
        recommendedQualityFlow
    ) { appSettings, recommended ->
        SettingsUiState(appSettings = appSettings, recommendedSpaceQuality = recommended)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(recommendedSpaceQuality = recommendedQuality)
    )

    fun setReducedMotionOverride(enabled: Boolean?) {
        viewModelScope.launch { repository.setReducedMotionOverride(enabled) }
    }

    /** Pass null to switch back to auto (device-recommended) quality. */
    fun setSpaceQualityOverride(quality: SpaceQuality?) {
        viewModelScope.launch { repository.setSpaceQualityOverride(quality) }
    }

    fun setTelemetryEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setTelemetryEnabled(enabled) }
    }

    fun setCrashReportsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setCrashReportsEnabled(enabled) }
    }
}
