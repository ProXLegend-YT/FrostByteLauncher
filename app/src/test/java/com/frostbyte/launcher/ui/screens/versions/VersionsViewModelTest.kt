package com.frostbyte.launcher.ui.screens.versions

import com.frostbyte.launcher.core.download.FakeDownloadJobScheduler
import com.frostbyte.launcher.core.filesystem.FakeGameDirectoryProvider
import com.frostbyte.launcher.core.network.model.DownloadArtifact
import com.frostbyte.launcher.core.network.model.LatestVersions
import com.frostbyte.launcher.core.network.model.VersionDetailResponse
import com.frostbyte.launcher.core.network.model.VersionDownloads
import com.frostbyte.launcher.core.network.model.VersionManifestEntry
import com.frostbyte.launcher.core.network.model.VersionManifestResponse
import com.frostbyte.launcher.core.network.service.FakeMojangMetaService
import com.frostbyte.launcher.core.storage.db.DownloadStatus
import com.frostbyte.launcher.core.storage.repository.DownloadRepository
import com.frostbyte.launcher.core.storage.repository.FakeDownloadDao
import com.frostbyte.launcher.core.storage.repository.FakeVersionCacheDao
import com.frostbyte.launcher.core.storage.repository.VersionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException

class VersionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mojangService: FakeMojangMetaService
    private lateinit var versionRepository: VersionRepository
    private lateinit var downloadRepository: DownloadRepository
    private lateinit var scheduler: FakeDownloadJobScheduler
    private lateinit var tempDir: File
    private lateinit var viewModel: VersionsViewModel

    private val detailUrl = "https://example.com/1.21.1.json"

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mojangService = FakeMojangMetaService()
        mojangService.manifestResponse = VersionManifestResponse(
            latest = LatestVersions(release = "1.21.1", snapshot = "24w33a"),
            versions = listOf(
                VersionManifestEntry(
                    id = "1.21.1",
                    type = "release",
                    url = detailUrl,
                    time = "2024-08-08T12:24:47+00:00",
                    releaseTime = "2024-08-08T12:24:47+00:00",
                    sha1 = "manifestsha1"
                )
            )
        )
        versionRepository = VersionRepository(mojangService, FakeVersionCacheDao())
        downloadRepository = DownloadRepository(FakeDownloadDao())
        scheduler = FakeDownloadJobScheduler()
        tempDir = File.createTempFile("frostbyte-vm-test", "").apply { delete(); mkdirs() }

        viewModel = VersionsViewModel(
            versionRepository = versionRepository,
            downloadRepository = downloadRepository,
            downloadScheduler = scheduler,
            fileManager = FakeGameDirectoryProvider(tempDir)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        tempDir.deleteRecursively()
    }

    @Test
    fun `downloadClientJar enqueues a real download with resolved sha1 and size`() = runTest(testDispatcher) {
        mojangService.detailResponsesByUrl[detailUrl] = VersionDetailResponse(
            id = "1.21.1",
            type = "release",
            mainClass = "net.minecraft.client.main.Main",
            downloads = VersionDownloads(
                client = DownloadArtifact(sha1 = "clientsha1", size = 99L, url = "https://example.com/client.jar")
            ),
            javaVersion = null,
            assetIndex = null
        )
        advanceUntilIdle() // let the init{}-triggered refresh() finish
        val version = viewModel.uiState.first { it.versions.isNotEmpty() }.versions.first()

        viewModel.downloadClientJar(version)
        advanceUntilIdle()

        val finalState = viewModel.uiState.first()
        assertEquals(1, scheduler.enqueuedIds.size)
        val downloadId = scheduler.enqueuedIds.first()
        val queued = downloadRepository.getById(downloadId)
        assertNotNull(queued)
        assertEquals("https://example.com/client.jar", queued!!.url)
        assertEquals("clientsha1", queued.expectedSha1)
        assertEquals(99L, queued.expectedSizeBytes)
        assertEquals(DownloadStatus.QUEUED, queued.status)
        assertTrue(queued.destinationPath.contains("1.21.1"))
        assertNull(finalState.resolvingDownloadForVersionId)
    }

    @Test
    fun `downloadClientJar surfaces an error and does not enqueue on resolve failure`() = runTest(testDispatcher) {
        mojangService.detailError = IOException("network down")
        advanceUntilIdle() // let the init{}-triggered refresh() finish
        val version = viewModel.uiState.first { it.versions.isNotEmpty() }.versions.first()

        viewModel.downloadClientJar(version)
        advanceUntilIdle()

        val finalState = viewModel.uiState.first()
        assertEquals(0, scheduler.enqueuedIds.size)
        assertNotNull(finalState.errorMessage)
        assertNull(finalState.resolvingDownloadForVersionId)
    }
}
