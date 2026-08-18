package com.frostbyte.launcher.core.modloader

import com.frostbyte.launcher.core.common.FrostByteResult
import com.frostbyte.launcher.core.common.Loader
import com.frostbyte.launcher.core.network.model.LibraryDownloads
import com.frostbyte.launcher.core.network.model.LibraryEntry
import com.frostbyte.launcher.core.network.model.LoaderProfileResponse
import com.frostbyte.launcher.core.network.model.LoaderVersionEntry
import com.frostbyte.launcher.core.network.model.LoaderVersionInfo
import com.frostbyte.launcher.core.network.service.FakeFabricMetaService
import com.frostbyte.launcher.core.network.service.FakeQuiltMetaService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class FabricQuiltRepositoryTest {

    private lateinit var fabricService: FakeFabricMetaService
    private lateinit var quiltService: FakeQuiltMetaService
    private lateinit var repository: FabricQuiltRepository

    @Before
    fun setUp() {
        fabricService = FakeFabricMetaService()
        quiltService = FakeQuiltMetaService()
        repository = FabricQuiltRepository(fabricService, quiltService)
    }

    private fun loaderEntry(version: String, stable: Boolean) = LoaderVersionEntry(
        loader = LoaderVersionInfo(separator = ".", build = 1, maven = "net.fabricmc:fabric-loader:$version", version = version, stable = stable)
    )

    @Test
    fun `getAvailableVersions returns real versions from Fabric meta`() = runTest {
        fabricService.versionsByMcVersion["1.21.1"] = listOf(
            loaderEntry("0.16.9", stable = true),
            loaderEntry("0.16.10-beta", stable = false)
        )

        val result = repository.getAvailableVersions(Loader.FABRIC, "1.21.1")

        assertTrue(result is FrostByteResult.Success)
        val versions = (result as FrostByteResult.Success).value
        assertEquals(2, versions.size)
        assertEquals("0.16.9", versions.first().version)
        assertTrue(versions.first().stable)
        assertTrue(!versions.last().stable)
    }

    @Test
    fun `getAvailableVersions works identically for Quilt via its own service`() = runTest {
        quiltService.versionsByMcVersion["1.21.1"] = listOf(loaderEntry("0.27.0", stable = true))

        val result = repository.getAvailableVersions(Loader.QUILT, "1.21.1")

        assertTrue(result is FrostByteResult.Success)
        assertEquals("0.27.0", (result as FrostByteResult.Success).value.first().version)
    }

    @Test
    fun `getAvailableVersions surfaces network failure without throwing`() = runTest {
        fabricService.error = IOException("no connection")

        val result = repository.getAvailableVersions(Loader.FABRIC, "1.21.1")

        assertTrue(result is FrostByteResult.Failure)
    }

    @Test
    fun `getAvailableVersions rejects non-Fabric-Quilt loaders`() = runTest {
        try {
            repository.getAvailableVersions(Loader.FORGE, "1.21.1")
            org.junit.Assert.fail("Expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            // expected
        }
    }

    @Test
    fun `resolveInstall returns real main class and additional libraries`() = runTest {
        fabricService.profilesByKey["1.21.1:0.16.9"] = LoaderProfileResponse(
            id = "fabric-loader-0.16.9-1.21.1",
            inheritsFrom = "1.21.1",
            mainClass = "net.fabricmc.loader.impl.launch.knot.KnotClient",
            libraries = listOf(
                LibraryEntry(name = "net.fabricmc:fabric-loader:0.16.9", downloads = LibraryDownloads(artifact = null))
            )
        )

        val result = repository.resolveInstall(Loader.FABRIC, "1.21.1", "0.16.9")

        assertTrue(result is FrostByteResult.Success)
        val install = (result as FrostByteResult.Success).value
        assertEquals("net.fabricmc.loader.impl.launch.knot.KnotClient", install.mainClass)
        assertEquals(1, install.additionalLibraries.size)
        assertEquals(Loader.FABRIC, install.loader)
        assertEquals("1.21.1", install.minecraftVersion)
    }

    @Test
    fun `resolveInstall surfaces a descriptive failure when the profile is missing`() = runTest {
        // No profile configured for this key at all.
        val result = repository.resolveInstall(Loader.FABRIC, "1.21.1", "9.9.9")

        assertTrue(result is FrostByteResult.Failure)
        assertTrue((result as FrostByteResult.Failure).message.contains("9.9.9"))
    }
}
