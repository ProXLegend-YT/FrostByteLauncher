package com.frostbyte.launcher.core.modloader

import com.frostbyte.launcher.core.common.FrostByteResult
import com.frostbyte.launcher.core.network.model.ForgePromotionsResponse
import com.frostbyte.launcher.core.network.model.NeoForgeVersionsResponse
import com.frostbyte.launcher.core.network.service.FakeForgeMetaService
import com.frostbyte.launcher.core.network.service.FakeNeoForgeMetaService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class ForgeVersionRepositoryTest {

    private lateinit var forgeService: FakeForgeMetaService
    private lateinit var neoForgeService: FakeNeoForgeMetaService
    private lateinit var repository: ForgeVersionRepository

    @Before
    fun setUp() {
        forgeService = FakeForgeMetaService()
        neoForgeService = FakeNeoForgeMetaService()
        repository = ForgeVersionRepository(forgeService, neoForgeService)
    }

    @Test
    fun `getForgeVersions extracts recommended and latest for the requested Minecraft version`() = runTest {
        forgeService.promotionsResponse = ForgePromotionsResponse(
            promos = mapOf(
                "1.21.1-recommended" to "52.0.1",
                "1.21.1-latest" to "52.0.2",
                "1.20.1-recommended" to "47.3.0" // a different MC version - must not leak into the 1.21.1 result
            )
        )

        val result = repository.getForgeVersions("1.21.1")

        assertTrue(result is FrostByteResult.Success)
        val listing = (result as FrostByteResult.Success).value
        assertEquals("52.0.1", listing.recommended)
        assertEquals("52.0.2", listing.latest)
    }

    @Test
    fun `getForgeVersions returns nulls when this Minecraft version has no Forge build`() = runTest {
        forgeService.promotionsResponse = ForgePromotionsResponse(promos = mapOf("1.20.1-recommended" to "47.3.0"))

        val result = repository.getForgeVersions("1.21.1")

        assertTrue(result is FrostByteResult.Success)
        val listing = (result as FrostByteResult.Success).value
        assertEquals(null, listing.recommended)
        assertEquals(null, listing.latest)
    }

    @Test
    fun `getForgeVersions surfaces network failure without throwing`() = runTest {
        forgeService.error = IOException("no connection")

        val result = repository.getForgeVersions("1.21.1")

        assertTrue(result is FrostByteResult.Failure)
    }

    @Test
    fun `getNeoForgeVersions filters by the Minecraft version prefix`() = runTest {
        neoForgeService.versionsResponse = NeoForgeVersionsResponse(
            isSnapshot = false,
            versions = listOf("21.1.7", "21.1.8", "21.4.0", "20.6.1") // last two belong to other MC versions
        )

        val result = repository.getNeoForgeVersions("1.21.1")

        assertTrue(result is FrostByteResult.Success)
        val versions = (result as FrostByteResult.Success).value
        assertEquals(listOf("21.1.7", "21.1.8"), versions)
    }

    @Test
    fun `getNeoForgeVersions returns an empty list when nothing matches, not an error`() = runTest {
        neoForgeService.versionsResponse = NeoForgeVersionsResponse(isSnapshot = false, versions = listOf("20.6.1"))

        val result = repository.getNeoForgeVersions("1.21.1")

        assertTrue(result is FrostByteResult.Success)
        assertTrue((result as FrostByteResult.Success).value.isEmpty())
    }
}
