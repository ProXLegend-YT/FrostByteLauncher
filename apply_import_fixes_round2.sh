#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "Syncing repo..."
git pull --rebase origin main

echo "Writing SettingsScreen.kt..."
cat > app/src/main/java/com/frostbyte/launcher/ui/screens/settings/SettingsScreen.kt << 'FILE_EOF'
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
FILE_EOF

echo "Writing VersionsScreen.kt..."
cat > app/src/main/java/com/frostbyte/launcher/ui/screens/versions/VersionsScreen.kt << 'FILE_EOF'
package com.frostbyte.launcher.ui.screens.versions

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
                    Column {
                        Text(
                            text = "Versions",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Manage your Minecraft Java Edition installations",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
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
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search versions...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IceBlue,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f)
                    )
                )
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
                        isSelected = uiState.selectedVersion?.id == version.id,
                        isResolving = uiState.resolvingDownloadForVersionId == version.id,
                        onRowClick = { viewModel.selectVersion(version.id) },
                        onDownloadClick = { viewModel.downloadClientJar(version) }
                    )
                }

                val selected = uiState.selectedVersion
                if (selected != null) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        VersionDetailPanel(
                            version = selected,
                            isResolving = uiState.resolvingDownloadForVersionId == selected.id,
                            onDownloadClick = { viewModel.downloadClientJar(selected) }
                        )
                    }
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
private fun VersionRow(
    version: MinecraftVersion,
    isSelected: Boolean,
    isResolving: Boolean,
    onRowClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (isSelected) {
                    Modifier.border(width = 1.5.dp, color = IceBlue, shape = shape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onRowClick)
    ) {
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
}

/**
 * Detail panel for the selected version, matching the reference design's
 * "Installation Info" card. Only shows fields MinecraftVersion actually has
 * (id, type, release date, SHA-1) - no fabricated Java/RAM/loader values,
 * since those belong to a Profile, not a raw downloadable version.
 */
@Composable
private fun VersionDetailPanel(version: MinecraftVersion, isResolving: Boolean, onDownloadClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "Version Info",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow(label = "ID", value = version.id)
            InfoRow(label = "Type", value = version.type.name.lowercase().replaceFirstChar(Char::uppercase))
            InfoRow(label = "Released", value = formatReleaseDate(version.releaseTimeEpochMillis))
            InfoRow(label = "SHA-1", value = version.sha1.take(12) + "…")
            Spacer(modifier = Modifier.height(16.dp))

            if (isResolving) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = IceBlue, strokeWidth = 2.dp)
                }
            } else {
                androidx.compose.material3.Button(
                    onClick = onDownloadClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = IceBlue,
                        contentColor = androidx.compose.ui.graphics.Color(0xFF04040C)
                    )
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Download client.jar")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
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
FILE_EOF

echo "Writing ContentBrowserScreen.kt..."
cat > app/src/main/java/com/frostbyte/launcher/ui/content/ContentBrowserScreen.kt << 'FILE_EOF'
package com.frostbyte.launcher.ui.content

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.frostbyte.launcher.core.content.ContentSearchResult
import com.frostbyte.launcher.ui.components.GlassCard
import com.frostbyte.launcher.ui.space.SpaceRenderer
import com.frostbyte.launcher.ui.theme.IceBlue
import com.frostbyte.launcher.ui.theme.StatusRed
import com.frostbyte.launcher.ui.theme.TextSecondary

@Composable
fun ContentBrowserScreen(title: String, viewModel: ContentBrowserViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        SpaceRenderer(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column {
                    Text(text = title, style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.onBackground)
                    Text(
                        text = "Search and install $title from Modrinth",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }

            item {
                OutlinedTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search $title...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = TextSecondary) },
                    trailingIcon = {
                        IconButton(onClick = viewModel::search) {
                            if (uiState.isSearching) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = IceBlue, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Search, contentDescription = "Search", tint = IceBlue)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = IceBlue,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.3f)
                    )
                )
            }

            if (uiState.errorMessage != null) {
                item {
                    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
                        Text(uiState.errorMessage!!, color = StatusRed, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (uiState.results.isEmpty() && !uiState.isSearching) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        GlassCard {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Extension, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Search to discover $title from Modrinth",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            } else {
                items(uiState.results, key = { it.id }) { item ->
                    ContentResultRow(
                        item = item,
                        isDownloading = uiState.downloadingProjectId == item.id,
                        onDownloadClick = { viewModel.downloadLatest(item) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentResultRow(item: ContentSearchResult, isDownloading: Boolean, onDownloadClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 14.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Placeholder glyph avatar - item.iconUrl is real data from Modrinth
            // but this build doesn't have an image-loading library wired up
            // yet, so rather than add a new dependency inside a UI restyle
            // (real risk of a broken build on a CI-only workflow), this shows
            // a consistent icon instead of nothing. Swapping in the real icon
            // via Coil/AsyncImage is a clean, self-contained follow-up.
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color = IceBlue.copy(alpha = 0.15f), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Extension, contentDescription = null, tint = IceBlue, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${formatDownloadCount(item.downloadCount)} downloads",
                    style = MaterialTheme.typography.labelSmall,
                    color = IceBlue
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (isDownloading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = IceBlue, strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onDownloadClick) {
                    Icon(Icons.Filled.Download, contentDescription = "Download ${item.title}", tint = IceBlue)
                }
            }
        }
    }
}

/** "12,483" -> "12.5k", ">=1,000,000 -> "1.2M". Real count, just formatted for a small screen. */
private fun formatDownloadCount(count: Int): String = when {
    count >= 1_000_000 -> "%.1fM".format(count / 1_000_000f)
    count >= 1_000 -> "%.1fk".format(count / 1_000f)
    else -> count.toString()
}
FILE_EOF

echo "Committing and pushing..."
git add -A
git commit -m "Fix missing Modifier extension imports (padding, width) across Settings/Versions/ContentBrowser screens"
git push

echo "Done!"
