package com.frostbyte.launcher.ui.screens.performance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostbyte.launcher.core.diagnostics.DeviceCapabilities
import com.frostbyte.launcher.core.diagnostics.DeviceCapabilitiesProvider
import com.frostbyte.launcher.core.diagnostics.SpaceQualityAdvisor
import com.frostbyte.launcher.core.storage.datastore.SettingsRepository
import com.frostbyte.launcher.ui.theme.SpaceQuality
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PerformanceUiState(
    val capabilities: DeviceCapabilities? = null,
    val recommendedQuality: SpaceQuality = SpaceQuality.BALANCED,
    val qualityOverride: SpaceQuality? = null
) {
    val effectiveQuality: SpaceQuality get() = qualityOverride ?: recommendedQuality
}

/**
 * Device capabilities don't change during a session, so they're queried
 * once at construction time rather than re-queried on every settings
 * emission - only the user's quality override (from Settings, Phase 2)
 * needs to stay reactive.
 */
class PerformanceViewModel(
    deviceCapabilitiesProvider: DeviceCapabilitiesProvider,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val capabilities = deviceCapabilitiesProvider.detect()
    private val recommended = SpaceQualityAdvisor.recommend(capabilities)

    val uiState: StateFlow<PerformanceUiState> = settingsRepository.settings
        .map { settings ->
            PerformanceUiState(
                capabilities = capabilities,
                recommendedQuality = recommended,
                qualityOverride = settings.spaceQualityOverride
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PerformanceUiState(capabilities = capabilities, recommendedQuality = recommended)
        )

    fun setQualityOverride(quality: SpaceQuality?) {
        viewModelScope.launch { settingsRepository.setSpaceQualityOverride(quality) }
    }
}
