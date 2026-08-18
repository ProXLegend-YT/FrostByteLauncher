package com.frostbyte.launcher.ui.screens.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostbyte.launcher.core.download.DownloadJobScheduler
import com.frostbyte.launcher.core.storage.repository.DownloadItem
import com.frostbyte.launcher.core.storage.repository.DownloadRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DownloadsViewModel(
    private val repository: DownloadRepository,
    private val scheduler: DownloadJobScheduler
) : ViewModel() {

    val downloads: StateFlow<List<DownloadItem>> = repository.observeDownloads().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun retry(item: DownloadItem) {
        scheduler.enqueue(item.id)
    }

    fun cancel(item: DownloadItem) {
        scheduler.cancel(item.id)
        viewModelScope.launch { repository.remove(item.id) }
    }

    fun clearCompleted() {
        viewModelScope.launch { repository.clearCompleted() }
    }
}
