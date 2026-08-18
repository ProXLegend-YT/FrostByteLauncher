package com.frostbyte.launcher.ui.screens.home

import com.frostbyte.launcher.core.auth.AuthRepository
import com.frostbyte.launcher.core.auth.FakeSessionStore
import com.frostbyte.launcher.core.auth.MicrosoftAuthConfig
import com.frostbyte.launcher.core.auth.MinecraftSession
import com.frostbyte.launcher.core.common.FrostByteResult
import com.frostbyte.launcher.core.common.Loader
import com.frostbyte.launcher.core.network.service.FakeMicrosoftAuthService
import com.frostbyte.launcher.core.network.service.FakeMinecraftAuthService
import com.frostbyte.launcher.core.network.service.FakeXboxAuthService
import com.frostbyte.launcher.core.storage.repository.FakeProfileDao
import com.frostbyte.launcher.core.storage.repository.ProfileDraft
import com.frostbyte.launcher.core.storage.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var dao: FakeProfileDao
    private lateinit var repository: ProfileRepository
    private lateinit var sessionStore: FakeSessionStore
    private lateinit var authRepository: AuthRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dao = FakeProfileDao()
        repository = ProfileRepository(dao)
        sessionStore = FakeSessionStore()
        authRepository = AuthRepository(
            microsoftAuthService = FakeMicrosoftAuthService(),
            xboxAuthService = FakeXboxAuthService(),
            minecraftAuthService = FakeMinecraftAuthService(),
            sessionStore = sessionStore,
            config = MicrosoftAuthConfig(clientId = "test-client-id"),
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun draft(name: String) = ProfileDraft(
        name = name,
        minecraftVersion = "1.21.1",
        loader = Loader.FABRIC,
        ramAllocationMb = 4096,
        gameDirectory = "profiles/$name"
    )

    private fun signedInSession() = MinecraftSession(
        minecraftUuid = "uuid-123",
        minecraftUsername = "Steve",
        minecraftAccessToken = "token",
        minecraftAccessTokenExpiresAtEpochMillis = Long.MAX_VALUE,
        msRefreshToken = "refresh"
    )

    @Test
    fun `no profiles yields null active profile`() = runTest(testDispatcher) {
        val viewModel = HomeViewModel(repository, authRepository)
        val state = viewModel.uiState.first()
        assertNull(state.activeProfile)
    }

    @Test
    fun `default-flagged profile is chosen as active even if not most recent`() = runTest(testDispatcher) {
        val recentId = (repository.createProfile(draft("Recent")) as FrostByteResult.Success).value
        val defaultId = (repository.createProfile(draft("Default")) as FrostByteResult.Success).value
        repository.setAsDefault(defaultId)
        repository.markPlayedNow(recentId)

        val viewModel = HomeViewModel(repository, authRepository)
        val state = viewModel.uiState.first { it.activeProfile != null }

        assertEquals(defaultId, state.activeProfile?.id)
        // The other profile should show up in recentProfiles, not duplicated as active.
        assertEquals(1, state.recentProfiles.size)
        assertEquals(recentId, state.recentProfiles.first().id)
    }

    @Test
    fun `falls back to first profile when none is flagged default`() = runTest(testDispatcher) {
        val onlyId = (repository.createProfile(draft("Solo")) as FrostByteResult.Success).value

        val viewModel = HomeViewModel(repository, authRepository)
        val state = viewModel.uiState.first { it.activeProfile != null }

        assertEquals(onlyId, state.activeProfile?.id)
        assertEquals(0, state.recentProfiles.size)
    }

    @Test
    fun `onPlayClicked reports NotReady mentioning Microsoft when nobody is signed in`() = runTest(testDispatcher) {
        repository.createProfile(draft("Solo"))
        val viewModel = HomeViewModel(repository, authRepository)
        viewModel.uiState.first { it.activeProfile != null } // wait for the profile to load

        viewModel.onPlayClicked()
        val state = viewModel.uiState.first { it.launchState is LaunchState.NotReady }

        val notReady = state.launchState as LaunchState.NotReady
        assertTrue(notReady.reason.contains("Microsoft account"))
    }

    @Test
    fun `onPlayClicked reports the real launch-not-wired reason when signed in`() = runTest(testDispatcher) {
        sessionStore.save(signedInSession())
        repository.createProfile(draft("Solo"))
        val viewModel = HomeViewModel(repository, authRepository)
        viewModel.uiState.first { it.activeProfile != null }

        viewModel.onPlayClicked()
        val state = viewModel.uiState.first { it.launchState is LaunchState.NotReady }

        val notReady = state.launchState as LaunchState.NotReady
        assertTrue(notReady.reason.contains("Steve"))
        assertTrue(notReady.reason.contains("isn't wired up yet"))
    }

    @Test
    fun `onPlayClicked does nothing when there is no active profile`() = runTest(testDispatcher) {
        val viewModel = HomeViewModel(repository, authRepository)
        viewModel.uiState.first() // ensure the flow has started collecting

        viewModel.onPlayClicked()

        // Give any (incorrectly) launched coroutine a chance to run, then
        // confirm the state is still Idle - there is nothing to launch.
        val state = viewModel.uiState.first()
        assertEquals(LaunchState.Idle, state.launchState)
    }

    @Test
    fun `dismissNotReady returns to Idle`() = runTest(testDispatcher) {
        repository.createProfile(draft("Solo"))
        val viewModel = HomeViewModel(repository, authRepository)
        viewModel.uiState.first { it.activeProfile != null }

        viewModel.onPlayClicked()
        viewModel.uiState.first { it.launchState is LaunchState.NotReady }

        viewModel.dismissNotReady()

        val state = viewModel.uiState.first { it.launchState is LaunchState.Idle }
        assertEquals(LaunchState.Idle, state.launchState)
    }
}
