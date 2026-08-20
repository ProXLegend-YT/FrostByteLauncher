package com.frostbyte.launcher.ui.screens.profiles

import com.frostbyte.launcher.core.auth.AuthRepository
import com.frostbyte.launcher.core.auth.FakeSessionStore
import com.frostbyte.launcher.core.auth.MicrosoftAuthConfig
import com.frostbyte.launcher.core.common.Loader
import com.frostbyte.launcher.core.network.service.FakeMicrosoftAuthService
import com.frostbyte.launcher.core.network.service.FakeMinecraftAuthService
import com.frostbyte.launcher.core.network.service.FakeXboxAuthService
import com.frostbyte.launcher.core.storage.repository.FakeProfileDao
import com.frostbyte.launcher.core.storage.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProfilesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ProfilesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val authRepository = AuthRepository(
            microsoftAuthService = FakeMicrosoftAuthService(),
            xboxAuthService = FakeXboxAuthService(),
            minecraftAuthService = FakeMinecraftAuthService(),
            sessionStore = FakeSessionStore(),
            config = MicrosoftAuthConfig(clientId = "test-client-id"),
            ioDispatcher = testDispatcher
        )
        viewModel = ProfilesViewModel(ProfileRepository(FakeProfileDao()), authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `dialog starts closed`() = runTest(testDispatcher) {
        assertFalse(viewModel.uiState.first().isCreateDialogOpen)
    }

    @Test
    fun `openCreateDialog and dismissCreateDialog toggle dialog state`() = runTest(testDispatcher) {
        // uiState.value stays at the stateIn() placeholder until the combine()
        // upstream emits at least once, so wait for the real emission rather
        // than reading .value synchronously right after a transientState update.
        viewModel.openCreateDialog()
        assertTrue(viewModel.uiState.first { it.isCreateDialogOpen }.isCreateDialogOpen)

        viewModel.dismissCreateDialog()
        assertFalse(viewModel.uiState.first { !it.isCreateDialogOpen }.isCreateDialogOpen)
    }

    @Test
    fun `createProfile with valid input adds profile and closes dialog`() = runTest(testDispatcher) {
        viewModel.openCreateDialog()
        viewModel.createProfile("Survival", "1.21.1", Loader.FABRIC, ramGb = 4)
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.profiles.isNotEmpty() }
        assertFalse(state.isCreateDialogOpen)
        assertEquals(1, state.profiles.size)
        assertEquals("Survival", state.profiles.first().name)
        assertEquals(4096, state.profiles.first().ramAllocationMb)
    }

    @Test
    fun `createProfile with blank name surfaces error and keeps dialog open`() = runTest(testDispatcher) {
        viewModel.openCreateDialog()
        viewModel.createProfile("   ", "1.21.1", Loader.FABRIC, ramGb = 4)
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.errorMessage != null }
        assertTrue(state.isCreateDialogOpen)
        assertNotNull(state.errorMessage)
        assertEquals(0, state.profiles.size)
    }

    @Test
    fun `dismissError clears the error message`() = runTest(testDispatcher) {
        viewModel.createProfile("", "1.21.1", Loader.FABRIC, ramGb = 4)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.first { it.errorMessage != null }.errorMessage)

        viewModel.dismissError()
        assertEquals(null, viewModel.uiState.first { it.errorMessage == null }.errorMessage)
    }
}
