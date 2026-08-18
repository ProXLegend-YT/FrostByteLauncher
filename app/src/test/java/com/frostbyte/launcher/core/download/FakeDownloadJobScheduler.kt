package com.frostbyte.launcher.core.download

class FakeDownloadJobScheduler : DownloadJobScheduler {
    val enqueuedIds = mutableListOf<Long>()
    val cancelledIds = mutableListOf<Long>()

    override fun enqueue(downloadId: Long) {
        enqueuedIds += downloadId
    }

    override fun cancel(downloadId: Long) {
        cancelledIds += downloadId
    }
}
