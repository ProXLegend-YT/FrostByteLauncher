package com.frostbyte.launcher.ui.screens.performance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.frostbyte.launcher.core.diagnostics.DeviceCapabilities
import com.frostbyte.launcher.ui.common.frostByteViewModel
import com.frostbyte.launcher.ui.components.GlassCard
import com.frostbyte.launcher.ui.space.SpaceRenderer
import com.frostbyte.launcher.ui.theme.SpaceQuality
import com.frostbyte.launcher.ui.theme.StatusAmber
import com.frostbyte.launcher.ui.theme.TextSecondary

@Composable
fun PerformanceScreen(
    viewModel: PerformanceViewModel = frostByteViewModel { container ->
        PerformanceViewModel(container.deviceCapabilitiesProvider, container.settingsRepository)
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        SpaceRenderer(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(text = "Performance", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onBackground)
            }

            uiState.capabilities?.let { caps ->
                item { DeviceCapabilitiesCard(caps) }
            }

            item {
                QualityControlCard(
                    recommended = uiState.recommendedQuality,
                    override = uiState.qualityOverride,
                    onSelect = viewModel::setQualityOverride
                )
            }
        }
    }
}

@Composable
private fun DeviceCapabilitiesCard(caps: DeviceCapabilities) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Device", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            StatRow("CPU cores", "${caps.cpuCoreCount}")
            StatRow("Total RAM", "${caps.totalRamMb} MB")
            StatRow("Available RAM", "${caps.availableRamMb} MB")
            StatRow("Free storage", "${caps.freeStorageMb} MB of ${caps.totalStorageMb} MB")
            if (caps.isLowRamDevice) {
                Text(
                    "Android has flagged this as a low-RAM device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = StatusAmber
                )
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun QualityControlCard(recommended: SpaceQuality, override: SpaceQuality?, onSelect: (SpaceQuality?) -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text("Space Rendering Quality", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(
                if (override == null) "Auto - recommended for this device: ${recommended.label()}" else "Manually set to ${override.label()}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = override == null, onClick = { onSelect(null) }, label = { Text("Auto") })
                SpaceQuality.entries.forEach { quality ->
                    FilterChip(selected = override == quality, onClick = { onSelect(quality) }, label = { Text(quality.label()) })
                }
            }
        }
    }
}

private fun SpaceQuality.label(): String = name.lowercase().replaceFirstChar(Char::uppercase)
