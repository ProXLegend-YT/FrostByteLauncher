package com.frostbyte.launcher.ui.content

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostbyte.launcher.core.common.FrostByteResult
import com.frostbyte.launcher.core.content.ContentSearchResult
import com.frostbyte.launcher.core.content.ContentType
import com.frostbyte.launcher.core.content.ModrinthContentRepository
import com.frostbyte.launcher.core.download.DownloadJobScheduler
import com.frostbyte.launcher.core.filesystem.GameDirectoryProvider
import com.frostbyte.launcher.core.storage.repository.DownloadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class ContentBrowserUiState(
    val query: String = "",
    val results: List<ContentSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val errorMessage: String? = null,
    val downloadingProjectId: String? = null
)

/**
 * Shared browser ViewModel for Mods/Shaders/Resource Packs (Section 10-12
 * of the PRD) - all three are structurally the same operation (search
 * Modrinth, filter by content type, download the resolved primary file into
 * the right on-disk folder), so one real, tested implementation backs all
 * three screens rather than three near-duplicate ViewModels.
 */
class ContentBrowserViewModel(
    private val contentType: ContentType,
    private val repository: ModrinthContentRepository,
    private val downloadRepository: DownloadRepository,
    private val downloadScheduler: DownloadJobScheduler,
    private val fileManager: GameDirectoryProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContentBrowserUiState())
    val uiState: StateFlow<ContentBrowserUiState> = _uiState.asStateFlow()

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
    }

    fun search() {
        val query = _uiState.value.query
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, errorMessage = null) }
            when (val result = repository.search(query, contentType)) {
                is FrostByteResult.Success ->
                    _uiState.update { it.copy(isSearching = false, results = result.value) }
                is FrostByteResult.Failure ->
                    _uiState.update { it.copy(isSearching = false, errorMessage = result.message) }
            }
        }
    }

    fun downloadLatest(item: ContentSearchResult) {
        if (_uiState.value.downloadingProjectId != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(downloadingProjectId = item.id, errorMessage = null) }

            val versionsResult = repository.getVersions(item.id)
            val version = when (versionsResult) {
                is FrostByteResult.Success -> versionsResult.value.firstOrNull()
                is FrostByteResult.Failure -> {
                    _uiState.update { it.copy(downloadingProjectId = null, errorMessage = versionsResult.message) }
                    return@launch
                }
            }
            if (version == null) {
                _uiState.update { it.copy(downloadingProjectId = null, errorMessage = "No downloadable version found for ${item.title}") }
                return@launch
            }

            val destinationDir = when (contentType) {
                ContentType.MOD -> fileManager.modsCacheDir()
                ContentType.SHADER -> fileManager.shadersCacheDir()
                ContentType.RESOURCE_PACK -> fileManager.resourcePacksCacheDir()
            }
            val destination = File(destinationDir, version.filename)

            val downloadId = downloadRepository.enqueue(
                url = version.fileUrl,
                destinationPath = destination.absolutePath,
                expectedSha1 = version.sha1,
                expectedSizeBytes = version.sizeBytes,
                label = "${item.title} ${version.versionNumber}"
            )
            downloadScheduler.enqueue(downloadId)
            _uiState.update { it.copy(downloadingProjectId = null) }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
