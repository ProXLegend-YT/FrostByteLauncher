package com.frostbyte.launcher.core.download

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Narrow interface for triggering/cancelling a download job. Lets
 * ViewModels (VersionsViewModel, DownloadsViewModel) be unit-tested with a
 * fake scheduler instead of needing a real WorkManager instance.
 */
interface DownloadJobScheduler {
    fun enqueue(downloadId: Long)
    fun cancel(downloadId: Long)
}

/**
 * Thin wrapper around WorkManager for scheduling DownloadWorker jobs.
 * Kept separate from DownloadRepository (pure data access) so the
 * WorkManager-specific scheduling concerns (constraints, backoff, unique
 * work naming) live in one obvious place.
 */
class DownloadScheduler(private val context: Context) : DownloadJobScheduler {

    override fun enqueue(downloadId: Long) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(Data.Builder().putLong(DownloadWorker.KEY_DOWNLOAD_ID, downloadId).build())
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
            .addTag(workTagFor(downloadId))
            .build()

        // Unique work per download id prevents accidentally double-enqueuing
        // the same download (e.g. user taps "retry" twice quickly).
        WorkManager.getInstance(context).enqueueUniqueWork(
            workTagFor(downloadId),
            androidx.work.ExistingWorkPolicy.KEEP,
            request
        )
    }

    override fun cancel(downloadId: Long) {
        WorkManager.getInstance(context).cancelUniqueWork(workTagFor(downloadId))
    }

    private fun workTagFor(downloadId: Long) = "download-$downloadId"
}
