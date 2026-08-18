package com.frostbyte.launcher.core.storage.repository

import com.frostbyte.launcher.core.common.FrostByteResult
import com.frostbyte.launcher.core.network.model.DownloadArtifact
import com.frostbyte.launcher.core.network.model.LatestVersions
import com.frostbyte.launcher.core.network.model.MinecraftVersionType
import com.frostbyte.launcher.core.network.model.VersionDetailResponse
import com.frostbyte.launcher.core.network.model.VersionDownloads
import com.frostbyte.launcher.core.network.model.VersionManifestEntry
import com.frostbyte.launcher.core.network.model.VersionManifestResponse
import com.frostbyte.launcher.core.network.service.FakeMojangMetaService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class VersionRepositoryTest {

    private lateinit var service: FakeMojangMetaService
    private lateinit var cacheDao: FakeVersionCacheDao
    private lateinit var repository: VersionRepository

    @Before
    fun setUp() {
        service = FakeMojangMetaService()
        cacheDao = FakeVersionCacheDao()
        repository = VersionRepository(service, cacheDao)
    }

    private fun entry(id: String, type: String = "release") = VersionManifestEntry(
        id = id,
        type = type,
        url = "https://example.com/$id.json",
        time = "2024-08-08T12:24:47+00:00",
        releaseTime = "2024-08-08T12:24:47+00:00",
        sha1 = "abc123"
    )

    private fun version(detailUrl: String) = MinecraftVersion(
        id = "1.21.1",
        type = MinecraftVersionType.RELEASE,
        releaseTimeEpochMillis = 0L,
        detailUrl = detailUrl,
        sha1 = "abc123"
    )

    @Test
    fun `refreshFromNetwork populates cache on success`() = runTest {
        service.manifestResponse = VersionManifestResponse(
            latest = LatestVersions(release = "1.21.1", snapshot = "24w33a"),
            versions = listOf(entry("1.21.1"), entry("24w33a", type = "snapshot"))
        )

        val result = repository.refreshFromNetwork()

        assertTrue(result is FrostByteResult.Success)
        assertEquals(2, (result as FrostByteResult.Success).value)
        assertEquals(2, repository.observeCachedVersions().first().size)
    }

    @Test
    fun `refreshFromNetwork failure leaves previously cached data intact`() = runTest {
        // First, a successful sync populates the cache.
        service.manifestResponse = VersionManifestResponse(
            latest = LatestVersions(release = "1.21.1", snapshot = "24w33a"),
            versions = listOf(entry("1.21.1"))
        )
        repository.refreshFromNetwork()
        assertEquals(1, repository.observeCachedVersions().first().size)

        // Then the network starts failing - cache should be untouched.
        service.manifestResponse = null
        service.manifestError = IOException("no network")

        val result = repository.refreshFromNetwork()

        assertTrue(result is FrostByteResult.Failure)
        assertEquals(1, repository.observeCachedVersions().first().size)
    }

    @Test
    fun `resolveClientDownload returns real artifact info on success`() = runTest {
        val detailUrl = "https://example.com/1.21.1.json"
        service.detailResponsesByUrl[detailUrl] = VersionDetailResponse(
            id = "1.21.1",
            type = "release",
            mainClass = "net.minecraft.client.main.Main",
            downloads = VersionDownloads(
                client = DownloadArtifact(sha1 = "deadbeef", size = 12345L, url = "https://example.com/client.jar")
            ),
            javaVersion = null,
            assetIndex = null
        )

        val result = repository.resolveClientDownload(version(detailUrl))

        assertTrue(result is FrostByteResult.Success)
        val info = (result as FrostByteResult.Success).value
        assertEquals("https://example.com/client.jar", info.url)
        assertEquals("deadbeef", info.sha1)
        assertEquals(12345L, info.sizeBytes)
    }

    @Test
    fun `resolveClientDownload surfaces failure without throwing`() = runTest {
        service.detailError = IOException("timeout")

        val result = repository.resolveClientDownload(version("https://example.com/1.21.1.json"))

        assertTrue(result is FrostByteResult.Failure)
    }
}
