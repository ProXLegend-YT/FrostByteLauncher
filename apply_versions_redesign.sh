#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "Syncing repo..."
git pull --rebase origin main

echo "Writing VersionsViewModel.kt..."
cat > app/src/main/java/com/frostbyte/launcher/ui/screens/versions/VersionsViewModel.kt << 'FILE_EOF'
package com.frostbyte.launcher.ui.screens.versions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostbyte.launcher.core.common.FrostByteResult
import com.frostbyte.launcher.core.download.DownloadJobScheduler
import com.frostbyte.launcher.core.filesystem.GameDirectoryProvider
import com.frostbyte.launcher.core.network.model.MinecraftVersionType
import com.frostbyte.launcher.core.storage.repository.DownloadRepository
import com.frostbyte.launcher.core.storage.repository.MinecraftVersion
import com.frostbyte.launcher.core.storage.repository.VersionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

enum class VersionFilter { ALL, RELEASE, SNAPSHOT }

private data class TransientState(
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val filter: VersionFilter = VersionFilter.RELEASE,
    val hasSyncedOnce: Boolean = false,
    val resolvingDownloadForVersionId: String? = null,
    val searchQuery: String = "",
    val selectedVersionId: String? = null
)

data class VersionsUiState(
    val versions: List<MinecraftVersion> = emptyList(),
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val filter: VersionFilter = VersionFilter.RELEASE,
    val hasSyncedOnce: Boolean = false,
    val resolvingDownloadForVersionId: String? = null,
    val searchQuery: String = "",
    val selectedVersionId: String? = null
) {
    val filteredVersions: List<MinecraftVersion>
        get() {
            val byType = when (filter) {
                VersionFilter.ALL -> versions
                VersionFilter.RELEASE -> versions.filter { it.type == MinecraftVersionType.RELEASE }
                VersionFilter.SNAPSHOT -> versions.filter { it.type == MinecraftVersionType.SNAPSHOT }
            }
            return if (searchQuery.isBlank()) {
                byType
            } else {
                byType.filter { it.id.contains(searchQuery, ignoreCase = true) }
            }
        }

    /** The version currently shown in the detail panel, defaulting to the first visible result. */
    val selectedVersion: MinecraftVersion?
        get() = filteredVersions.firstOrNull { it.id == selectedVersionId } ?: filteredVersions.firstOrNull()
}

/**
 * Versions screen ViewModel. "Download" here means: resolve the version's
 * real client jar URL/SHA-1/size from Mojang, enqueue it in the Download
 * queue (Room), and schedule a real WorkManager job to fetch it - the same
 * pipeline the Downloads screen displays. This is the first real end-to-end
 * connection between two Phase 3 subsystems (Version Manager -> Download
 * Manager) rather than each living in isolation.
 */
class VersionsViewModel(
    private val versionRepository: VersionRepository,
    private val downloadRepository: DownloadRepository,
    private val downloadScheduler: DownloadJobScheduler,
    private val fileManager: GameDirectoryProvider
) : ViewModel() {

    private val transientState = MutableStateFlow(TransientState())

    val uiState: StateFlow<VersionsUiState> = combine(
        versionRepository.observeCachedVersions(),
        transientState
    ) { versions, transient ->
        VersionsUiState(
            versions = versions,
            isRefreshing = transient.isRefreshing,
            errorMessage = transient.errorMessage,
            filter = transient.filter,
            hasSyncedOnce = transient.hasSyncedOnce,
            resolvingDownloadForVersionId = transient.resolvingDownloadForVersionId,
            searchQuery = transient.searchQuery,
            selectedVersionId = transient.selectedVersionId
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = VersionsUiState()
    )

    init {
        refresh()
    }

    fun refresh() {
        if (transientState.value.isRefreshing) return
        viewModelScope.launch {
            transientState.update { it.copy(isRefreshing = true, errorMessage = null) }
            when (val result = versionRepository.refreshFromNetwork()) {
                is FrostByteResult.Success ->
                    transientState.update { it.copy(isRefreshing = false, hasSyncedOnce = true) }
                is FrostByteResult.Failure ->
                    transientState.update { it.copy(isRefreshing = false, errorMessage = result.message) }
            }
        }
    }

    fun setFilter(filter: VersionFilter) {
        transientState.update { it.copy(filter = filter) }
    }

    fun setSearchQuery(query: String) {
        transientState.update { it.copy(searchQuery = query) }
    }

    fun selectVersion(versionId: String) {
        transientState.update { it.copy(selectedVersionId = versionId) }
    }

    fun dismissError() {
        transientState.update { it.copy(errorMessage = null) }
    }

    fun downloadClientJar(version: MinecraftVersion) {
        if (transientState.value.resolvingDownloadForVersionId != null) return

        viewModelScope.launch {
            transientState.update { it.copy(resolvingDownloadForVersionId = version.id, errorMessage = null) }

            when (val result = versionRepository.resolveClientDownload(version)) {
                is FrostByteResult.Success -> {
                    val info = result.value
                    val destination = File(fileManager.versionsDir(), "${version.id}/client.jar")
                    val downloadId = downloadRepository.enqueue(
                        url = info.url,
                        destinationPath = destination.absolutePath,
                        expectedSha1 = info.sha1,
                        expectedSizeBytes = info.sizeBytes,
                        label = "Minecraft ${version.id} client"
                    )
                    downloadScheduler.enqueue(downloadId)
                    transientState.update { it.copy(resolvingDownloadForVersionId = null) }
                }
                is FrostByteResult.Failure -> {
                    transientState.update {
                        it.copy(resolvingDownloadForVersionId = null, errorMessage = result.message)
                    }
                }
            }
        }
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

echo "Committing and pushing..."
git add -A
git commit -m "Redesign Versions screen: search bar, tappable rows, detail info panel matching reference design"
git push

echo "Done!"
