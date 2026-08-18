package com.frostbyte.launcher.core.storage.repository

import com.frostbyte.launcher.core.common.FrostByteResult
import com.frostbyte.launcher.core.common.Loader
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProfileRepositoryTest {

    private lateinit var dao: FakeProfileDao
    private lateinit var repository: ProfileRepository

    @Before
    fun setUp() {
        dao = FakeProfileDao()
        repository = ProfileRepository(dao)
    }

    private fun validDraft(name: String = "Survival") = ProfileDraft(
        name = name,
        minecraftVersion = "1.21.1",
        loader = Loader.FABRIC,
        ramAllocationMb = 4096,
        gameDirectory = "profiles/survival"
    )

    @Test
    fun `createProfile rejects blank name`() = runTest {
        val result = repository.createProfile(validDraft(name = "  "))
        assertTrue(result is FrostByteResult.Failure)
    }

    @Test
    fun `createProfile rejects non-positive RAM`() = runTest {
        val result = repository.createProfile(validDraft().copy(ramAllocationMb = 0))
        assertTrue(result is FrostByteResult.Failure)
    }

    @Test
    fun `createProfile succeeds and appears in observeProfiles`() = runTest {
        val result = repository.createProfile(validDraft())
        assertTrue(result is FrostByteResult.Success)

        val profiles = repository.observeProfiles().first()
        assertEquals(1, profiles.size)
        assertEquals("Survival", profiles.first().name)
        assertEquals(4f, profiles.first().ramAllocationGb)
    }

    @Test
    fun `setAsDefault clears previous default`() = runTest {
        val firstId = (repository.createProfile(validDraft("First")) as FrostByteResult.Success).value
        val secondId = (repository.createProfile(validDraft("Second")) as FrostByteResult.Success).value

        repository.setAsDefault(firstId)
        assertEquals(firstId, repository.getDefaultProfile()?.id)

        repository.setAsDefault(secondId)
        val default = repository.getDefaultProfile()
        assertEquals(secondId, default?.id)

        // Exactly one profile should be flagged default at a time.
        val allProfiles = repository.observeProfiles().first()
        assertEquals(1, allProfiles.count { it.isDefault })
    }

    @Test
    fun `deleteProfile removes it from storage`() = runTest {
        val id = (repository.createProfile(validDraft()) as FrostByteResult.Success).value
        val profile = repository.getProfile(id)!!

        repository.deleteProfile(profile)

        assertNull(repository.getProfile(id))
        assertTrue(repository.observeProfiles().first().isEmpty())
    }

    @Test
    fun `markPlayedNow sets a non-null timestamp`() = runTest {
        val id = (repository.createProfile(validDraft()) as FrostByteResult.Success).value
        assertNull(repository.getProfile(id)!!.lastPlayedEpochMillis)

        repository.markPlayedNow(id)

        assertFalse(repository.getProfile(id)!!.lastPlayedEpochMillis == null)
    }

    @Test
    fun `updateProfile rejects blank name`() = runTest {
        val id = (repository.createProfile(validDraft()) as FrostByteResult.Success).value
        val profile = repository.getProfile(id)!!

        val result = repository.updateProfile(profile.copy(name = ""))

        assertTrue(result is FrostByteResult.Failure)
    }
}
