package com.frostbyte.launcher.core.download

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class FileDownloaderTest {

    private lateinit var server: MockWebServer
    private lateinit var downloader: FileDownloader
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        downloader = FileDownloader(OkHttpClient())
        tempDir = File.createTempFile("frostbyte-test", "").apply {
            delete()
            mkdirs()
        }
    }

    @After
    fun tearDown() {
        server.shutdown()
        tempDir.deleteRecursively()
    }

    private fun sha1Of(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-1").digest(bytes).joinToString("") { "%02x".format(it) }

    @Test
    fun `downloads a file and verifies matching checksum`() = runTest {
        val content = "Hello FrostByte".toByteArray()
        server.enqueue(MockResponse().setBody(okio.Buffer().write(content)).setResponseCode(200))

        val destination = File(tempDir, "out.bin")
        val result = downloader.download(
            url = server.url("/file").toString(),
            destination = destination,
            expectedSha1 = sha1Of(content),
            expectedSizeBytes = content.size.toLong(),
            onProgress = { _, _ -> }
        )

        assertEquals(DownloadOutcome.Success, result)
        assertEquals(content.toList(), destination.readBytes().toList())
    }

    @Test
    fun `checksum mismatch is reported, not silently accepted`() = runTest {
        val content = "Hello FrostByte".toByteArray()
        server.enqueue(MockResponse().setBody(okio.Buffer().write(content)).setResponseCode(200))

        val destination = File(tempDir, "out.bin")
        val result = downloader.download(
            url = server.url("/file").toString(),
            destination = destination,
            expectedSha1 = "0000000000000000000000000000000000000",
            expectedSizeBytes = content.size.toLong(),
            onProgress = { _, _ -> }
        )

        assertTrue(result is DownloadOutcome.ChecksumMismatch)
    }

    @Test
    fun `http error is surfaced as HttpError`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))

        val destination = File(tempDir, "out.bin")
        val result = downloader.download(
            url = server.url("/missing").toString(),
            destination = destination,
            expectedSha1 = null,
            expectedSizeBytes = 0,
            onProgress = { _, _ -> }
        )

        assertEquals(DownloadOutcome.HttpError(404), result)
    }

    @Test
    fun `resumes a partial download via Range header`() = runTest {
        val fullContent = "0123456789ABCDEF".toByteArray() // 16 bytes
        val alreadyHave = fullContent.copyOfRange(0, 8) // first 8 bytes "already downloaded"
        val remaining = fullContent.copyOfRange(8, fullContent.size)

        val destination = File(tempDir, "resume.bin")
        destination.writeBytes(alreadyHave)

        // Server honors the Range request with a 206 + only the remaining bytes.
        server.enqueue(
            MockResponse()
                .setResponseCode(206)
                .setBody(okio.Buffer().write(remaining))
        )

        val result = downloader.download(
            url = server.url("/file").toString(),
            destination = destination,
            expectedSha1 = sha1Of(fullContent),
            expectedSizeBytes = fullContent.size.toLong(),
            onProgress = { _, _ -> }
        )

        val recordedRequest = server.takeRequest()
        assertEquals("bytes=8-", recordedRequest.getHeader("Range"))
        assertEquals(DownloadOutcome.Success, result)
        assertEquals(fullContent.toList(), destination.readBytes().toList())
    }

    @Test
    fun `already-complete file is not re-downloaded`() = runTest {
        val content = "Complete already".toByteArray()
        val destination = File(tempDir, "complete.bin").apply { writeBytes(content) }

        // No response enqueued at all - if the downloader tried to hit the
        // network, MockWebServer would throw due to an empty queue.
        val result = downloader.download(
            url = server.url("/file").toString(),
            destination = destination,
            expectedSha1 = sha1Of(content),
            expectedSizeBytes = content.size.toLong(),
            onProgress = { _, _ -> }
        )

        assertEquals(DownloadOutcome.Success, result)
        assertEquals(0, server.requestCount)
    }
}
