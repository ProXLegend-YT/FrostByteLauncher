package com.frostbyte.launcher.ui.screens.accounts

import com.frostbyte.launcher.core.auth.AuthRepository
import com.frostbyte.launcher.core.auth.FakeSessionStore
import com.frostbyte.launcher.core.auth.MicrosoftAuthConfig
import com.frostbyte.launcher.core.auth.MinecraftSession
import com.frostbyte.launcher.core.network.model.MinecraftLoginResponse
import com.frostbyte.launcher.core.network.model.MinecraftProfileResponse
import com.frostbyte.launcher.core.network.model.MsDeviceCodeResponse
import com.frostbyte.launcher.core.network.model.MsTokenResponse
import com.frostbyte.launcher.core.network.model.XboxLiveAuthResponse
import com.frostbyte.launcher.core.network.model.XboxLiveDisplayClaims
import com.frostbyte.launcher.core.network.model.XboxLiveUserInfo
import com.frostbyte.launcher.core.network.service.FakeMicrosoftAuthService
import com.frostbyte.launcher.core.network.service.FakeMinecraftAuthService
import com.frostbyte.launcher.core.network.service.FakeXboxAuthService
import com.frostbyte.launcher.core.network.service.successResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AccountsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var msService: FakeMicrosoftAuthService
    private lateinit var xboxService: FakeXboxAuthService
    private lateinit var mcService: FakeMinecraftAuthService
    private lateinit var sessionStore: FakeSessionStore
    private lateinit var authRepository: AuthRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        msService = FakeMicrosoftAuthService()
        xboxService = FakeXboxAuthService()
        mcService = FakeMinecraftAuthService()
        sessionStore = FakeSessionStore()
        authRepository = AuthRepository(
            microsoftAuthService = msService,
            xboxAuthService = xboxService,
            minecraftAuthService = mcService,
            sessionStore = sessionStore,
            config = MicrosoftAuthConfig(clientId = "test-client-id"),
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `starts with no current session when nothing is stored`() = runTest(testDispatcher) {
        val viewModel = AccountsViewModel(authRepository)
        assertNull(viewModel.uiState.value.currentSession)
    }

    @Test
    fun `startSignIn walks the real chain and promotes the session on success`() = runTest(testDispatcher) {
        msService.deviceCodeResponse = MsDeviceCodeResponse("dc", "code", "https://ms.com", 900, 1)
        msService.pollResponses = mutableListOf(successResponse(MsTokenResponse("ms-token", "ms-refresh", 3600, "Bearer")))
        xboxService.xboxLiveResponse = XboxLiveAuthResponse("xbl", XboxLiveDisplayClaims(listOf(XboxLiveUserInfo("uhs"))))
        xboxService.xstsResponse = successResponse(XboxLiveAuthResponse("xsts", XboxLiveDisplayClaims(listOf(XboxLiveUserInfo("uhs")))))
        mcService.loginResponse = MinecraftLoginResponse("mc-token", "Bearer", 86400)
        mcService.profileResponse = successResponse(MinecraftProfileResponse("uuid-123", "Steve"))

        val viewModel = AccountsViewModel(authRepository)
        viewModel.startSignIn()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("Steve", viewModel.uiState.value.currentSession?.minecraftUsername)
    }

    @Test
    fun `signOut clears the current session`() = runTest(testDispatcher) {
        sessionStore.save(
            MinecraftSession(
                minecraftUuid = "uuid",
                minecraftUsername = "Steve",
                minecraftAccessToken = "token",
                minecraftAccessTokenExpiresAtEpochMillis = Long.MAX_VALUE,
                msRefreshToken = "refresh"
            )
        )
        val viewModel = AccountsViewModel(authRepository)
        assertEquals("Steve", viewModel.uiState.value.currentSession?.minecraftUsername)

        viewModel.signOut()

        assertNull(viewModel.uiState.value.currentSession)
        assertNull(sessionStore.load())
    }
}
