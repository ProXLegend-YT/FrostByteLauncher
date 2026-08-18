package com.frostbyte.launcher.core.auth

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
import com.frostbyte.launcher.core.network.service.errorResponse
import com.frostbyte.launcher.core.network.service.successResponse
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthRepositoryTest {

    private lateinit var msService: FakeMicrosoftAuthService
    private lateinit var xboxService: FakeXboxAuthService
    private lateinit var mcService: FakeMinecraftAuthService
    private lateinit var sessionStore: FakeSessionStore

    @Before
    fun setUp() {
        msService = FakeMicrosoftAuthService()
        xboxService = FakeXboxAuthService()
        mcService = FakeMinecraftAuthService()
        sessionStore = FakeSessionStore()
    }

    /**
     * Builds the repository with a StandardTestDispatcher backed by THIS
     * test's own testScheduler (via TestScope.testScheduler), not a
     * disconnected one - AuthRepository.signIn() internally calls
     * flowOn(ioDispatcher), and a dispatcher on an unrelated scheduler would
     * either deadlock waiting on real delay() calls or silently skip
     * runTest's virtual-time advancement. Sharing the scheduler keeps the
     * device-code polling loop's delay() calls properly virtual-time-aware.
     */
    private fun TestScope.repository(configured: Boolean = true) = AuthRepository(
        microsoftAuthService = msService,
        xboxAuthService = xboxService,
        minecraftAuthService = mcService,
        sessionStore = sessionStore,
        config = MicrosoftAuthConfig(clientId = if (configured) "test-client-id" else ""),
        ioDispatcher = StandardTestDispatcher(testScheduler)
    )

    @Test
    fun `signIn fails immediately and honestly when no client ID is configured`() = runTest {
        val states = repository(configured = false).signIn().toList()

        assertEquals(1, states.size)
        val failed = states.first() as SignInState.Failed
        assertTrue(failed.reason.contains("not yet registered"))
        assertEquals(0, sessionStore.saveCallCount)
    }

    @Test
    fun `full chain success emits every real stage and persists the session`() = runTest {
        msService.deviceCodeResponse = MsDeviceCodeResponse(
            device_code = "dc-123",
            user_code = "ABCD-EFGH",
            verification_uri = "https://microsoft.com/devicelogin",
            expires_in = 900,
            interval = 1
        )
        msService.pollResponses = mutableListOf(
            successResponse(MsTokenResponse(access_token = "ms-token", refresh_token = "ms-refresh", expires_in = 3600, token_type = "Bearer"))
        )
        xboxService.xboxLiveResponse = XboxLiveAuthResponse(
            Token = "xbl-token",
            DisplayClaims = XboxLiveDisplayClaims(xui = listOf(XboxLiveUserInfo(uhs = "user-hash")))
        )
        xboxService.xstsResponse = successResponse(
            XboxLiveAuthResponse(Token = "xsts-token", DisplayClaims = XboxLiveDisplayClaims(xui = listOf(XboxLiveUserInfo(uhs = "user-hash"))))
        )
        mcService.loginResponse = MinecraftLoginResponse(access_token = "mc-access-token", token_type = "Bearer", expires_in = 86400)
        mcService.profileResponse = successResponse(MinecraftProfileResponse(id = "uuid-123", name = "Steve"))

        val states = repository().signIn().toList()

        assertTrue(states.any { it is SignInState.AwaitingUserAction })
        assertTrue(states.any { it is SignInState.ExchangingXboxLive })
        assertTrue(states.any { it is SignInState.ExchangingXsts })
        assertTrue(states.any { it is SignInState.LoggingIntoMinecraft })
        assertTrue(states.any { it is SignInState.VerifyingOwnership })

        val success = states.last() as SignInState.Success
        assertEquals("Steve", success.session.minecraftUsername)
        assertEquals("uuid-123", success.session.minecraftUuid)
        assertEquals("mc-access-token", success.session.minecraftAccessToken)
        assertEquals(1, sessionStore.saveCallCount)
        assertEquals(success.session, sessionStore.load())
    }

    @Test
    fun `404 on profile lookup is reported as a real ownership failure, never granted anyway`() = runTest {
        msService.deviceCodeResponse = MsDeviceCodeResponse("dc", "code", "https://ms.com", 900, 1)
        msService.pollResponses = mutableListOf(successResponse(MsTokenResponse("ms-token", "ms-refresh", 3600, "Bearer")))
        xboxService.xboxLiveResponse = XboxLiveAuthResponse("xbl-token", XboxLiveDisplayClaims(listOf(XboxLiveUserInfo("uhs"))))
        xboxService.xstsResponse = successResponse(XboxLiveAuthResponse("xsts-token", XboxLiveDisplayClaims(listOf(XboxLiveUserInfo("uhs")))))
        mcService.loginResponse = MinecraftLoginResponse("mc-token", "Bearer", 86400)
        mcService.profileResponse = errorResponse(404, "not found")

        val states = repository().signIn().toList()

        val failed = states.last() as SignInState.Failed
        assertTrue(failed.reason.contains("does not own Minecraft"))
        assertEquals(0, sessionStore.saveCallCount) // ownership failure must never result in a saved session
    }

    @Test
    fun `XSTS error 2148916233 is translated to a specific, actionable message`() = runTest {
        msService.deviceCodeResponse = MsDeviceCodeResponse("dc", "code", "https://ms.com", 900, 1)
        msService.pollResponses = mutableListOf(successResponse(MsTokenResponse("ms-token", "ms-refresh", 3600, "Bearer")))
        xboxService.xboxLiveResponse = XboxLiveAuthResponse("xbl-token", XboxLiveDisplayClaims(listOf(XboxLiveUserInfo("uhs"))))
        xboxService.xstsResponse = errorResponse(401, """{"XErr":2148916233,"Message":""}""")

        val states = repository().signIn().toList()

        val failed = states.last() as SignInState.Failed
        assertTrue(failed.reason.contains("no Xbox Live profile"))
    }

    @Test
    fun `authorization_pending keeps polling instead of failing`() = runTest {
        msService.deviceCodeResponse = MsDeviceCodeResponse("dc", "code", "https://ms.com", 900, 1)
        msService.pollResponses = mutableListOf(
            errorResponse<MsTokenResponse>(400, """{"error":"authorization_pending"}"""),
            errorResponse<MsTokenResponse>(400, """{"error":"authorization_pending"}"""),
            successResponse(MsTokenResponse("ms-token", "ms-refresh", 3600, "Bearer"))
        )
        xboxService.xboxLiveResponse = XboxLiveAuthResponse("xbl-token", XboxLiveDisplayClaims(listOf(XboxLiveUserInfo("uhs"))))
        xboxService.xstsResponse = successResponse(XboxLiveAuthResponse("xsts-token", XboxLiveDisplayClaims(listOf(XboxLiveUserInfo("uhs")))))
        mcService.loginResponse = MinecraftLoginResponse("mc-token", "Bearer", 86400)
        mcService.profileResponse = successResponse(MinecraftProfileResponse("uuid-123", "Steve"))

        val states = repository().signIn().toList()

        assertTrue(states.last() is SignInState.Success)
    }

    @Test
    fun `access_denied stops polling and fails immediately rather than retrying forever`() = runTest {
        msService.deviceCodeResponse = MsDeviceCodeResponse("dc", "code", "https://ms.com", 900, 1)
        msService.pollResponses = mutableListOf(errorResponse(400, """{"error":"access_denied"}"""))

        val states = repository().signIn().toList()

        assertTrue(states.last() is SignInState.Failed)
        assertEquals(0, sessionStore.saveCallCount)
    }
}
