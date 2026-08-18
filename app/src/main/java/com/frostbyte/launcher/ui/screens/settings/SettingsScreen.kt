package com.frostbyte.launcher.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.frostbyte.launcher.ui.common.frostByteViewModel
import com.frostbyte.launcher.ui.components.GlassCard
import com.frostbyte.launcher.ui.space.SpaceRenderer
import com.frostbyte.launcher.ui.theme.IceBlue
import com.frostbyte.launcher.ui.theme.SpaceQuality
import com.frostbyte.launcher.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = frostByteViewModel { container ->
        SettingsViewModel(container.settingsRepository, container.deviceCapabilitiesProvider)
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings = uiState.appSettings

    Box(modifier = Modifier.fillMaxSize()) {
        SpaceRenderer(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                SettingsSection(title = "Visuals") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column {
                            Text(
                                "Space rendering quality",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                if (settings.spaceQualityOverride == null) {
                                    "Auto - recommended for this device: ${uiState.recommendedSpaceQuality.label()}"
                                } else {
                                    "Manually set. Auto would recommend ${uiState.recommendedSpaceQuality.label()}."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = settings.spaceQualityOverride == null,
                                onClick = { viewModel.setSpaceQualityOverride(null) },
                                label = { Text("Auto") }
                            )
                            SpaceQuality.entries.forEach { quality ->
                                FilterChip(
                                    selected = settings.spaceQualityOverride == quality,
                                    onClick = { viewModel.setSpaceQualityOverride(quality) },
                                    label = { Text(quality.label()) }
                                )
                            }
                        }
                    }
                }
            }

            item {
                SettingsSection(title = "Accessibility") {
                    ToggleRow(
                        title = "Reduce motion",
                        subtitle = if (settings.reducedMotionOverride == null) {
                            "Following system setting"
                        } else {
                            "Manually overridden"
                        },
                        checked = settings.reducedMotionOverride ?: false,
                        onCheckedChange = { viewModel.setReducedMotionOverride(it) }
                    )
                }
            }

            item {
                SettingsSection(title = "Privacy") {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ToggleRow(
                            title = "Anonymous usage telemetry",
                            subtitle = "Off by default. Helps improve the launcher.",
                            checked = settings.telemetryEnabled,
                            onCheckedChange = viewModel::setTelemetryEnabled
                        )
                        ToggleRow(
                            title = "Crash reports",
                            subtitle = "Off by default. Never includes passwords or tokens.",
                            checked = settings.crashReportsEnabled,
                            onCheckedChange = viewModel::setCrashReportsEnabled
                        )
                    }
                }
            }
        }
    }
}

private fun SpaceQuality.label(): String = name.lowercase().replaceFirstChar(Char::uppercase)

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF04040C),
                checkedTrackColor = IceBlue
            )
        )
    }
}
