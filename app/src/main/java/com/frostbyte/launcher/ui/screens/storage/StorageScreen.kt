package com.frostbyte.launcher.ui.screens.storage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.frostbyte.launcher.core.backup.WorldBackupInfo
import com.frostbyte.launcher.core.filesystem.FileManager
import com.frostbyte.launcher.ui.common.frostByteViewModel
import com.frostbyte.launcher.ui.components.GlassCard
import com.frostbyte.launcher.ui.space.SpaceRenderer
import com.frostbyte.launcher.ui.theme.IceBlue
import com.frostbyte.launcher.ui.theme.StatusRed
import com.frostbyte.launcher.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StorageScreen(
    viewModel: StorageViewModel = frostByteViewModel { container ->
        StorageViewModel(container.fileManager, container.worldBackupManager)
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
                Text(text = "Storage", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onBackground)
            }

            if (uiState.isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = IceBlue)
                    }
                }
            } else {
                uiState.breakdown?.let { breakdown ->
                    item { StorageBreakdownCard(breakdown, onClearCache = viewModel::clearCache) }
                }

                item {
                    Text("World Backups", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                }
                if (uiState.backups.isEmpty()) {
                    item {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text("No world backups yet.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                } else {
                    items(uiState.backups, key = { it.file.absolutePath }) { backup ->
                        BackupRow(backup, onDelete = { viewModel.deleteBackup(backup) })
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageBreakdownCard(breakdown: FileManager.StorageBreakdown, onClearCache: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Disk Usage", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                TextButton(onClick = onClearCache) { Text("Clear cache") }
            }
            BreakdownRow("Versions", breakdown.versionsBytes)
            BreakdownRow("Libraries", breakdown.librariesBytes)
            BreakdownRow("Assets", breakdown.assetsBytes)
            BreakdownRow("Mods cache", breakdown.modsCacheBytes)
            BreakdownRow("Shaders cache", breakdown.shadersCacheBytes)
            BreakdownRow("Resource packs cache", breakdown.resourcePacksCacheBytes)
            BreakdownRow("Java runtimes", breakdown.javaRuntimesBytes)
            BreakdownRow("Logs", breakdown.logsBytes)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(formatBytes(breakdown.totalBytes), style = MaterialTheme.typography.titleMedium, color = IceBlue)
            }
        }
    }
}

@Composable
private fun BreakdownRow(label: String, bytes: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(formatBytes(bytes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun BackupRow(backup: WorldBackupInfo, onDelete: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(backup.worldName, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "${formatDate(backup.createdAtEpochMillis)} · ${formatBytes(backup.sizeBytes)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete backup", tint = StatusRed)
            }
        }
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> "%.2f GB".format(bytes / 1_073_741_824.0)
    bytes >= 1_048_576L -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1024L -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}

private fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("MMM d, yyyy h:mm a", Locale.US).format(Date(epochMillis))
