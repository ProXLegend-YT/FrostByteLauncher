package com.frostbyte.launcher.core.content

import com.frostbyte.launcher.core.common.FrostByteResult
import com.frostbyte.launcher.core.network.model.ModrinthFileHashes
import com.frostbyte.launcher.core.network.model.ModrinthSearchHit
import com.frostbyte.launcher.core.network.model.ModrinthSearchResponse
import com.frostbyte.launcher.core.network.model.ModrinthVersionFile
import com.frostbyte.launcher.core.network.model.ModrinthVersionResponse
import com.frostbyte.launcher.core.network.service.FakeModrinthService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class ModrinthContentRepositoryTest {

    private lateinit var service: FakeModrinthService
    private lateinit var repository: ModrinthContentRepository

    @Before
    fun setUp() {
        service = FakeModrinthService()
        repository = ModrinthContentRepository(service)
    }

    private fun hit(id: String, type: String) = ModrinthSearchHit(
        project_id = id,
        slug = "$id-slug",
        title = "$id title",
        description = "desc",
        icon_url = "https://example.com/$id.png",
        downloads = 100,
        project_type = type,
        categories = emptyList()
    )

    @Test
    fun `search maps hits to domain ContentSearchResult with correct content type`() = runTest {
        service.searchResponse = ModrinthSearchResponse(hits = listOf(hit("sodium", "mod")), total_hits = 1)

        val result = repository.search("sodium", ContentType.MOD)

        assertTrue(result is FrostByteResult.Success)
        val results = (result as FrostByteResult.Success).value
        assertEquals(1, results.size)
        assertEquals("sodium", results.first().id)
        assertEquals(ContentType.MOD, results.first().contentType)
    }

    @Test
    fun `search passes the correctly-built facets string through to the service`() = runTest {
        service.searchResponse = ModrinthSearchResponse(hits = emptyList(), total_hits = 0)

        repository.search("iris", ContentType.SHADER, minecraftVersion = "1.21.1", loader = "fabric")

        assertEquals(
            """[["project_type:shader"],["versions:1.21.1"],["categories:fabric"]]""",
            service.lastFacets
        )
    }

    @Test
    fun `unknown project_type from the API defaults to MOD instead of crashing`() = runTest {
        service.searchResponse = ModrinthSearchResponse(hits = listOf(hit("datapack-thing", "datapack")), total_hits = 1)

        val result = repository.search("thing", ContentType.MOD)

        assertTrue(result is FrostByteResult.Success)
        assertEquals(ContentType.MOD, (result as FrostByteResult.Success).value.first().contentType)
    }

    @Test
    fun `search surfaces network failure without throwing`() = runTest {
        service.error = IOException("down")

        val result = repository.search("sodium", ContentType.MOD)

        assertTrue(result is FrostByteResult.Failure)
    }

    @Test
    fun `getVersions picks the primary file when multiple files exist`() = runTest {
        service.versionsResponse = listOf(
            ModrinthVersionResponse(
                id = "v1",
                project_id = "sodium",
                name = "Sodium 0.5.8",
                version_number = "0.5.8",
                game_versions = listOf("1.21.1"),
                loaders = listOf("fabric"),
                files = listOf(
                    ModrinthVersionFile(
                        url = "https://cdn.modrinth.com/sources.jar",
                        filename = "sodium-sources.jar",
                        primary = false,
                        size = 10L,
                        hashes = ModrinthFileHashes(sha1 = "aaa", sha512 = "aaaa")
                    ),
                    ModrinthVersionFile(
                        url = "https://cdn.modrinth.com/sodium.jar",
                        filename = "sodium-0.5.8.jar",
                        primary = true,
                        size = 500L,
                        hashes = ModrinthFileHashes(sha1 = "bbb", sha512 = "bbbb")
                    )
                )
            )
        )

        val result = repository.getVersions("sodium")

        assertTrue(result is FrostByteResult.Success)
        val version = (result as FrostByteResult.Success).value.first()
        assertEquals("sodium-0.5.8.jar", version.filename)
        assertEquals("bbb", version.sha1)
        assertEquals(500L, version.sizeBytes)
    }

    @Test
    fun `getVersions falls back to the first file when none is marked primary`() = runTest {
        service.versionsResponse = listOf(
            ModrinthVersionResponse(
                id = "v1",
                project_id = "sodium",
                name = "Sodium",
                version_number = "0.5.8",
                game_versions = listOf("1.21.1"),
                loaders = listOf("fabric"),
                files = listOf(
                    ModrinthVersionFile(
                        url = "https://cdn.modrinth.com/only.jar",
                        filename = "only.jar",
                        primary = false,
                        size = 1L,
                        hashes = ModrinthFileHashes(sha1 = "x", sha512 = "xx")
                    )
                )
            )
        )

        val result = repository.getVersions("sodium")

        assertTrue(result is FrostByteResult.Success)
        assertEquals("only.jar", (result as FrostByteResult.Success).value.first().filename)
    }

    @Test
    fun `getVersions skips a version with no files at all rather than crashing`() = runTest {
        service.versionsResponse = listOf(
            ModrinthVersionResponse(
                id = "v-empty",
                project_id = "sodium",
                name = "broken",
                version_number = "0.0.0",
                game_versions = emptyList(),
                loaders = emptyList(),
                files = emptyList()
            )
        )

        val result = repository.getVersions("sodium")

        assertTrue(result is FrostByteResult.Success)
        assertTrue((result as FrostByteResult.Success).value.isEmpty())
    }
}
