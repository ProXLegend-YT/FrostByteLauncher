package com.frostbyte.launcher.core.diagnostics

import com.frostbyte.launcher.ui.theme.SpaceQuality
import org.junit.Assert.assertEquals
import org.junit.Test

class SpaceQualityAdvisorTest {

    private fun caps(
        ramMb: Long,
        cores: Int,
        isLowRam: Boolean = false
    ) = DeviceCapabilities(
        cpuCoreCount = cores,
        totalRamMb = ramMb,
        availableRamMb = ramMb / 2,
        isLowRamDevice = isLowRam,
        freeStorageMb = 10_000,
        totalStorageMb = 64_000
    )

    @Test
    fun `isLowRamDevice flag always forces LOW regardless of raw numbers`() {
        val result = SpaceQualityAdvisor.recommend(caps(ramMb = 16_000, cores = 12, isLowRam = true))
        assertEquals(SpaceQuality.LOW, result)
    }

    @Test
    fun `under 3GB ram recommends LOW`() {
        assertEquals(SpaceQuality.LOW, SpaceQualityAdvisor.recommend(caps(ramMb = 2_000, cores = 8)))
    }

    @Test
    fun `3 to 6GB ram recommends BALANCED`() {
        assertEquals(SpaceQuality.BALANCED, SpaceQualityAdvisor.recommend(caps(ramMb = 4_000, cores = 8)))
    }

    @Test
    fun `high ram but low core count is capped at BALANCED`() {
        assertEquals(SpaceQuality.BALANCED, SpaceQualityAdvisor.recommend(caps(ramMb = 10_000, cores = 4)))
    }

    @Test
    fun `6 to 8GB ram with enough cores recommends HIGH`() {
        assertEquals(SpaceQuality.HIGH, SpaceQualityAdvisor.recommend(caps(ramMb = 7_000, cores = 7)))
    }

    @Test
    fun `8GB plus ram and 8 plus cores recommends ULTRA`() {
        assertEquals(SpaceQuality.ULTRA, SpaceQualityAdvisor.recommend(caps(ramMb = 12_000, cores = 8)))
    }

    @Test
    fun `boundary at exactly 3000MB is BALANCED not LOW`() {
        assertEquals(SpaceQuality.BALANCED, SpaceQualityAdvisor.recommend(caps(ramMb = 3_000, cores = 8)))
    }

    @Test
    fun `boundary at exactly 8000MB and 8 cores is ULTRA not HIGH`() {
        assertEquals(SpaceQuality.ULTRA, SpaceQualityAdvisor.recommend(caps(ramMb = 8_000, cores = 8)))
    }
}
