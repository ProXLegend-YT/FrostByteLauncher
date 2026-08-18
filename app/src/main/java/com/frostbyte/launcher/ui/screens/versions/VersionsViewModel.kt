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
    val resolvingDownloadForVersionId: String? = null
)

data class VersionsUiState(
    val versions: List<MinecraftVersion> = emptyList(),
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val filter: VersionFilter = VersionFilter.RELEASE,
    val hasSyncedOnce: Boolean = false,
    val resolvingDownloadForVersionId: String? = null
) {
    val filteredVersions: List<MinecraftVersion>
        get() = when (filter) {
            VersionFilter.ALL -> versions
            VersionFilter.RELEASE -> versions.filter { it.type == MinecraftVersionType.RELEASE }
            VersionFilter.SNAPSHOT -> versions.filter { it.type == MinecraftVersionType.SNAPSHOT }
        }
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
            resolvingDownloadForVersionId = transient.resolvingDownloadForVersionId
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
