package com.frostbyte.launcher.ui.screens.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostbyte.launcher.core.backup.WorldBackupInfo
import com.frostbyte.launcher.core.backup.WorldBackupManager
import com.frostbyte.launcher.core.filesystem.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StorageUiState(
    val breakdown: FileManager.StorageBreakdown? = null,
    val isLoading: Boolean = true,
    val backups: List<WorldBackupInfo> = emptyList()
)

/**
 * FileManager.computeStorageBreakdown() genuinely walks the filesystem
 * (directorySizeBytes sums real file sizes recursively) - this is real
 * disk I/O, not a placeholder calculation, so it's dispatched off the main
 * thread here rather than called directly from a Composable.
 */
class StorageViewModel(
    private val fileManager: FileManager,
    private val worldBackupManager: WorldBackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageUiState())
    val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val breakdown = withContext(Dispatchers.IO) { fileManager.computeStorageBreakdown() }
            val backups = withContext(Dispatchers.IO) { worldBackupManager.listBackups() }
            _uiState.update { it.copy(breakdown = breakdown, backups = backups, isLoading = false) }
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { fileManager.clearCache() }
            refresh()
        }
    }

    fun deleteBackup(backup: WorldBackupInfo) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { worldBackupManager.deleteBackup(backup.file) }
            refresh()
        }
    }
}
