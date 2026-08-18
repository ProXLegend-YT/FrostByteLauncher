package com.frostbyte.launcher.ui.content

import com.frostbyte.launcher.core.content.ContentSearchResult
import com.frostbyte.launcher.core.content.ContentType
import com.frostbyte.launcher.core.content.ModrinthContentRepository
import com.frostbyte.launcher.core.download.FakeDownloadJobScheduler
import com.frostbyte.launcher.core.filesystem.FakeGameDirectoryProvider
import com.frostbyte.launcher.core.network.model.ModrinthFileHashes
import com.frostbyte.launcher.core.network.model.ModrinthSearchHit
import com.frostbyte.launcher.core.network.model.ModrinthSearchResponse
import com.frostbyte.launcher.core.network.model.ModrinthVersionFile
import com.frostbyte.launcher.core.network.model.ModrinthVersionResponse
import com.frostbyte.launcher.core.network.service.FakeModrinthService
import com.frostbyte.launcher.core.storage.repository.DownloadRepository
import com.frostbyte.launcher.core.storage.repository.FakeDownloadDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException

class ContentBrowserViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var modrinthService: FakeModrinthService
    private lateinit var downloadRepository: DownloadRepository
    private lateinit var scheduler: FakeDownloadJobScheduler
    private lateinit var tempDir: File
    private lateinit var viewModel: ContentBrowserViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        modrinthService = FakeModrinthService()
        downloadRepository = DownloadRepository(FakeDownloadDao())
        scheduler = FakeDownloadJobScheduler()
        tempDir = File.createTempFile("content-browser-test", "").apply { delete(); mkdirs() }

        viewModel = ContentBrowserViewModel(
            contentType = ContentType.MOD,
            repository = ModrinthContentRepository(modrinthService),
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

    private fun searchHit(id: String) = ModrinthSearchHit(
        project_id = id,
        slug = "$id-slug",
        title = "$id title",
        description = "desc",
        icon_url = null,
        downloads = 42,
        project_type = "mod",
        categories = emptyList()
    )

    private fun searchResult(id: String) = ContentSearchResult(
        id = id,
        slug = "$id-slug",
        title = "$id title",
        description = "desc",
        iconUrl = null,
        downloadCount = 42,
        contentType = ContentType.MOD
    )

    @Test
    fun `search populates results on success`() = runTest(testDispatcher) {
        modrinthService.searchResponse = ModrinthSearchResponse(hits = listOf(searchHit("sodium")), total_hits = 1)
        viewModel.onQueryChanged("sodium")

        viewModel.search()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.results.size)
        assertEquals("sodium", viewModel.uiState.value.results.first().id)
    }

    @Test
    fun `search surfaces an error message on failure`() = runTest(testDispatcher) {
        modrinthService.error = IOException("down")
        viewModel.onQueryChanged("sodium")

        viewModel.search()
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.errorMessage)
        assertTrue(viewModel.uiState.value.results.isEmpty())
    }

    @Test
    fun `downloadLatest enqueues the resolved primary file into the mods cache dir`() = runTest(testDispatcher) {
        modrinthService.versionsResponse = listOf(
            ModrinthVersionResponse(
                id = "v1",
                project_id = "sodium",
                name = "Sodium",
                version_number = "0.5.8",
                game_versions = listOf("1.21.1"),
                loaders = listOf("fabric"),
                files = listOf(
                    ModrinthVersionFile(
                        url = "https://cdn.modrinth.com/sodium.jar",
                        filename = "sodium-0.5.8.jar",
                        primary = true,
                        size = 500L,
                        hashes = ModrinthFileHashes(sha1 = "abc123", sha512 = "abcabc")
                    )
                )
            )
        )

        viewModel.downloadLatest(searchResult("sodium"))
        advanceUntilIdle()

        assertEquals(1, scheduler.enqueuedIds.size)
        val queued = downloadRepository.getById(scheduler.enqueuedIds.first())
        assertNotNull(queued)
        assertEquals("https://cdn.modrinth.com/sodium.jar", queued!!.url)
        assertEquals("abc123", queued.expectedSha1)
        assertTrue(queued.destinationPath.contains("mods_cache"))
        assertTrue(queued.destinationPath.contains("sodium-0.5.8.jar"))
    }

    @Test
    fun `downloadLatest reports an error when the project has no downloadable version`() = runTest(testDispatcher) {
        modrinthService.versionsResponse = emptyList()

        viewModel.downloadLatest(searchResult("empty-project"))
        advanceUntilIdle()

        assertEquals(0, scheduler.enqueuedIds.size)
        assertNotNull(viewModel.uiState.value.errorMessage)
    }
}
