package com.frostbyte.launcher.core.storage.repository

import com.frostbyte.launcher.core.storage.db.DownloadDao
import com.frostbyte.launcher.core.storage.db.DownloadEntity
import com.frostbyte.launcher.core.storage.db.DownloadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeDownloadDao : DownloadDao {
    private var nextId = 1L
    private val state = MutableStateFlow<List<DownloadEntity>>(emptyList())

    override fun observeAll(): StateFlow<List<DownloadEntity>> = state

    override suspend fun getById(id: Long): DownloadEntity? = state.value.firstOrNull { it.id == id }

    override suspend fun insert(download: DownloadEntity): Long {
        val id = nextId++
        state.value = state.value + download.copy(id = id)
        return id
    }

    override suspend fun update(download: DownloadEntity) {
        state.value = state.value.map { if (it.id == download.id) download else it }
    }

    override suspend fun updateStatus(id: Long, status: DownloadStatus, error: String?) {
        state.value = state.value.map { if (it.id == id) it.copy(status = status, errorMessage = error) else it }
    }

    override suspend fun updateProgress(id: Long, bytes: Long) {
        state.value = state.value.map { if (it.id == id) it.copy(downloadedBytes = bytes) else it }
    }

    override suspend fun delete(id: Long) {
        state.value = state.value.filterNot { it.id == id }
    }

    override suspend fun clearCompleted() {
        state.value = state.value.filterNot { it.status == DownloadStatus.COMPLETED }
    }
}
