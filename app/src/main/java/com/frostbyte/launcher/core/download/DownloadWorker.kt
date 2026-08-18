package com.frostbyte.launcher.core.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.frostbyte.launcher.FrostByteApplication
import com.frostbyte.launcher.core.storage.db.DownloadStatus
import java.io.File

/**
 * Executes one queued download in the background via WorkManager, per
 * Section 9 of the PRD (downloads must survive app backgrounding/process
 * death - WorkManager, not a raw coroutine tied to a ViewModel scope, is
 * what gives that guarantee).
 *
 * Reads its dependencies from FrostByteApplication's container rather than
 * constructor injection, since WorkManager instantiates Workers itself via
 * reflection (no custom WorkerFactory has been introduced yet - see
 * docs/KNOWN_GAPS.md if that becomes a real limitation later).
 */
class DownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val downloadId = inputData.getLong(KEY_DOWNLOAD_ID, -1L)
        if (downloadId == -1L) return Result.failure()

        val container = (applicationContext as FrostByteApplication).container
        val repository = container.downloadRepository
        val downloader = container.fileDownloader

        val item = repository.getById(downloadId) ?: return Result.failure()

        repository.markStatus(downloadId, DownloadStatus.DOWNLOADING)

        val outcome = try {
            downloader.download(
                url = item.url,
                destination = File(item.destinationPath),
                expectedSha1 = item.expectedSha1,
                expectedSizeBytes = item.expectedSizeBytes,
                onProgress = { downloaded, _ -> repository.updateProgress(downloadId, downloaded) }
            )
        } catch (e: Exception) {
            repository.markStatus(downloadId, DownloadStatus.FAILED, e.message ?: "Unknown error")
            return Result.failure()
        }

        return when (outcome) {
            is DownloadOutcome.Success -> {
                repository.markStatus(downloadId, DownloadStatus.COMPLETED)
                Result.success()
            }
            is DownloadOutcome.ChecksumMismatch -> {
                repository.markStatus(
                    downloadId,
                    DownloadStatus.FAILED,
                    "Checksum mismatch: expected ${outcome.expected}, got ${outcome.actual}"
                )
                // Deliberately not retried automatically - a checksum
                // mismatch means the downloaded bytes are wrong, and blindly
                // retrying the same request could just fail the same way
                // forever. Section 9 asks for "retry on failure," which
                // FrostByte interprets as user-initiated retry for
                // integrity failures, vs. automatic retry for transient
                // network errors (below).
                Result.failure()
            }
            is DownloadOutcome.HttpError -> {
                repository.markStatus(downloadId, DownloadStatus.FAILED, "HTTP ${outcome.code}")
                if (outcome.code in 500..599 && runAttemptCount < MAX_AUTO_RETRIES) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
            is DownloadOutcome.NetworkError -> {
                repository.markStatus(downloadId, DownloadStatus.FAILED, outcome.message)
                if (runAttemptCount < MAX_AUTO_RETRIES) {
                    Result.retry()
                } else {
                    Result.failure()
                }
            }
        }
    }

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
        private const val MAX_AUTO_RETRIES = 3
    }
}
