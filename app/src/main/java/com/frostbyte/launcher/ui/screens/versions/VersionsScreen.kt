package com.frostbyte.launcher.ui.screens.versions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.frostbyte.launcher.core.network.model.MinecraftVersionType
import com.frostbyte.launcher.core.storage.repository.MinecraftVersion
import com.frostbyte.launcher.ui.common.frostByteViewModel
import com.frostbyte.launcher.ui.components.GlassCard
import com.frostbyte.launcher.ui.space.SpaceRenderer
import com.frostbyte.launcher.ui.theme.IceBlue
import com.frostbyte.launcher.ui.theme.StatusAmber
import com.frostbyte.launcher.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VersionsScreen(
    viewModel: VersionsViewModel = frostByteViewModel { container ->
        VersionsViewModel(
            versionRepository = container.versionRepository,
            downloadRepository = container.downloadRepository,
            downloadScheduler = container.downloadScheduler,
            fileManager = container.fileManager
        )
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        SpaceRenderer(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Versions",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    IconButton(onClick = viewModel::refresh) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = IceBlue, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = IceBlue)
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VersionFilter.entries.forEach { filter ->
                        FilterChip(
                            selected = uiState.filter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(filter.name.lowercase().replaceFirstChar(Char::uppercase)) }
                        )
                    }
                }
            }

            if (uiState.errorMessage != null) {
                item { OfflineNotice(message = uiState.errorMessage!!, hasAnyCachedData = uiState.versions.isNotEmpty()) }
            }

            if (uiState.filteredVersions.isEmpty() && !uiState.isRefreshing) {
                item { EmptyVersionsState(hasSyncedOnce = uiState.hasSyncedOnce) }
            } else {
                items(uiState.filteredVersions, key = { it.id }) { version ->
                    VersionRow(
                        version = version,
                        isResolving = uiState.resolvingDownloadForVersionId == version.id,
                        onDownloadClick = { viewModel.downloadClientJar(version) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OfflineNotice(message: String, hasAnyCachedData: Boolean) {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.CloudOff, contentDescription = null, tint = StatusAmber)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(text = message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                if (hasAnyCachedData) {
                    Text(
                        text = "Showing previously cached versions",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyVersionsState(hasSyncedOnce: Boolean) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        GlassCard {
            Text(
                text = if (hasSyncedOnce) "No versions match this filter" else "Loading version list…",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun VersionRow(version: MinecraftVersion, isResolving: Boolean, onDownloadClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = version.id, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = formatReleaseDate(version.releaseTimeEpochMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            VersionTypeBadge(type = version.type)
            Spacer(modifier = Modifier.width(12.dp))
            if (isResolving) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = IceBlue, strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onDownloadClick) {
                    Icon(Icons.Filled.Download, contentDescription = "Download ${version.id}", tint = IceBlue)
                }
            }
        }
    }
}

@Composable
private fun VersionTypeBadge(type: MinecraftVersionType) {
    val (label, color) = when (type) {
        MinecraftVersionType.RELEASE -> "Release" to IceBlue
        MinecraftVersionType.SNAPSHOT -> "Snapshot" to StatusAmber
        MinecraftVersionType.OLD_BETA -> "Beta" to TextSecondary
        MinecraftVersionType.OLD_ALPHA -> "Alpha" to TextSecondary
        MinecraftVersionType.UNKNOWN -> "Unknown" to TextSecondary
    }
    Text(text = label, style = MaterialTheme.typography.labelLarge, color = color)
}

private fun formatReleaseDate(epochMillis: Long): String {
    val format = SimpleDateFormat("MMM d, yyyy", Locale.US)
    return format.format(Date(epochMillis))
}
