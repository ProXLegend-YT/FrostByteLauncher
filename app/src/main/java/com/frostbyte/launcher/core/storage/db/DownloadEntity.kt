package com.frostbyte.launcher.core.storage.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single download task tracked in the DB, per Section 9 of the PRD
 * (Download Manager: queued/parallel downloads, resumable, checksum
 * verification, retry on failure). This table is the source of truth for
 * "what's downloading and how far along is it" - WorkManager itself only
 * tracks whether the *worker* succeeded/failed, not byte-level progress or
 * queue ordering, so this entity is still needed alongside WorkManager.
 */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val destinationPath: String,
    val expectedSha1: String?,
    val expectedSizeBytes: Long,
    val downloadedBytes: Long = 0,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val errorMessage: String? = null,
    val label: String, // human-readable, e.g. "Minecraft 1.21.1 client jar"
    val createdAtEpochMillis: Long
)

enum class DownloadStatus {
    QUEUED, DOWNLOADING, VERIFYING, COMPLETED, FAILED, PAUSED, CANCELLED
}
