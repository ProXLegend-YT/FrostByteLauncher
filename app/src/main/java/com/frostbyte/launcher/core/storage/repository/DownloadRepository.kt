package com.frostbyte.launcher.core.storage.repository

import com.frostbyte.launcher.core.storage.db.DownloadDao
import com.frostbyte.launcher.core.storage.db.DownloadEntity
import com.frostbyte.launcher.core.storage.db.DownloadStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class DownloadItem(
    val id: Long,
    val url: String,
    val destinationPath: String,
    val expectedSha1: String?,
    val expectedSizeBytes: Long,
    val downloadedBytes: Long,
    val status: DownloadStatus,
    val errorMessage: String?,
    val label: String
) {
    val progressFraction: Float
        get() = if (expectedSizeBytes <= 0) 0f else (downloadedBytes.toFloat() / expectedSizeBytes).coerceIn(0f, 1f)
}

class DownloadRepository(private val dao: DownloadDao) {

    fun observeDownloads(): Flow<List<DownloadItem>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun enqueue(
        url: String,
        destinationPath: String,
        expectedSha1: String?,
        expectedSizeBytes: Long,
        label: String
    ): Long = dao.insert(
        DownloadEntity(
            url = url,
            destinationPath = destinationPath,
            expectedSha1 = expectedSha1,
            expectedSizeBytes = expectedSizeBytes,
            status = DownloadStatus.QUEUED,
            label = label,
            createdAtEpochMillis = System.currentTimeMillis()
        )
    )

    suspend fun getById(id: Long): DownloadItem? = dao.getById(id)?.toDomain()

    suspend fun markStatus(id: Long, status: DownloadStatus, error: String? = null) {
        dao.updateStatus(id, status, error)
    }

    suspend fun updateProgress(id: Long, downloadedBytes: Long) {
        dao.updateProgress(id, downloadedBytes)
    }

    suspend fun remove(id: Long) {
        dao.delete(id)
    }

    suspend fun clearCompleted() {
        dao.clearCompleted()
    }
}

private fun DownloadEntity.toDomain() = DownloadItem(
    id = id,
    url = url,
    destinationPath = destinationPath,
    expectedSha1 = expectedSha1,
    expectedSizeBytes = expectedSizeBytes,
    downloadedBytes = downloadedBytes,
    status = status,
    errorMessage = errorMessage,
    label = label
)
