package com.frostbyte.launcher.core.storage.repository

import com.frostbyte.launcher.core.storage.db.DownloadStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DownloadRepositoryTest {

    private lateinit var dao: FakeDownloadDao
    private lateinit var repository: DownloadRepository

    @Before
    fun setUp() {
        dao = FakeDownloadDao()
        repository = DownloadRepository(dao)
    }

    @Test
    fun `enqueue creates a QUEUED entry retrievable by its returned id`() = runTest {
        val id = repository.enqueue(
            url = "https://example.com/file.jar",
            destinationPath = "/data/file.jar",
            expectedSha1 = "abc123",
            expectedSizeBytes = 1000L,
            label = "Test file"
        )

        val item = repository.getById(id)

        assertEquals("https://example.com/file.jar", item?.url)
        assertEquals(DownloadStatus.QUEUED, item?.status)
        assertEquals(0L, item?.downloadedBytes)
    }

    @Test
    fun `markStatus updates status and error message`() = runTest {
        val id = repository.enqueue("https://example.com/f", "/f", null, 100L, "F")

        repository.markStatus(id, DownloadStatus.FAILED, "network error")

        val item = repository.getById(id)
        assertEquals(DownloadStatus.FAILED, item?.status)
        assertEquals("network error", item?.errorMessage)
    }

    @Test
    fun `updateProgress updates downloadedBytes only`() = runTest {
        val id = repository.enqueue("https://example.com/f", "/f", null, 1000L, "F")

        repository.updateProgress(id, 250L)

        val item = repository.getById(id)
        assertEquals(250L, item?.downloadedBytes)
        assertEquals(DownloadStatus.QUEUED, item?.status) // unaffected by a progress update
    }

    @Test
    fun `remove deletes the entry`() = runTest {
        val id = repository.enqueue("https://example.com/f", "/f", null, 100L, "F")

        repository.remove(id)

        assertNull(repository.getById(id))
    }

    @Test
    fun `clearCompleted removes only COMPLETED entries`() = runTest {
        val completedId = repository.enqueue("https://example.com/a", "/a", null, 100L, "A")
        val queuedId = repository.enqueue("https://example.com/b", "/b", null, 100L, "B")
        repository.markStatus(completedId, DownloadStatus.COMPLETED)

        repository.clearCompleted()

        assertNull(repository.getById(completedId))
        assertEquals(DownloadStatus.QUEUED, repository.getById(queuedId)?.status)
    }

    @Test
    fun `observeDownloads reflects the current set of entries`() = runTest {
        repository.enqueue("https://example.com/a", "/a", null, 100L, "A")
        repository.enqueue("https://example.com/b", "/b", null, 100L, "B")

        val all = repository.observeDownloads().first()

        assertEquals(2, all.size)
    }

    @Test
    fun `progressFraction is zero when expectedSizeBytes is zero, not a division error`() {
        val item = DownloadItem(
            id = 1, url = "u", destinationPath = "p", expectedSha1 = null,
            expectedSizeBytes = 0L, downloadedBytes = 500L,
            status = DownloadStatus.DOWNLOADING, errorMessage = null, label = "L"
        )
        assertEquals(0f, item.progressFraction)
    }

    @Test
    fun `progressFraction is a normal fraction for a partial download`() {
        val item = DownloadItem(
            id = 1, url = "u", destinationPath = "p", expectedSha1 = null,
            expectedSizeBytes = 1000L, downloadedBytes = 250L,
            status = DownloadStatus.DOWNLOADING, errorMessage = null, label = "L"
        )
        assertEquals(0.25f, item.progressFraction)
    }

    @Test
    fun `progressFraction is clamped to 1 even if downloadedBytes overshoots expectedSizeBytes`() {
        // Can genuinely happen with a slightly-off Content-Length header from a server.
        val item = DownloadItem(
            id = 1, url = "u", destinationPath = "p", expectedSha1 = null,
            expectedSizeBytes = 1000L, downloadedBytes = 1200L,
            status = DownloadStatus.DOWNLOADING, errorMessage = null, label = "L"
        )
        assertTrue(item.progressFraction <= 1f)
        assertEquals(1f, item.progressFraction)
    }
}
