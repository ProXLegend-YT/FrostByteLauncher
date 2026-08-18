package com.frostbyte.launcher.core.network.service

import com.frostbyte.launcher.core.network.model.MinecraftLoginRequest
import com.frostbyte.launcher.core.network.model.MinecraftLoginResponse
import com.frostbyte.launcher.core.network.model.MinecraftProfileResponse
import com.frostbyte.launcher.core.network.model.MsDeviceCodeResponse
import com.frostbyte.launcher.core.network.model.MsTokenResponse
import com.frostbyte.launcher.core.network.model.XboxLiveAuthRequest
import com.frostbyte.launcher.core.network.model.XboxLiveAuthResponse
import com.frostbyte.launcher.core.network.model.XstsAuthRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response

class FakeMicrosoftAuthService : MicrosoftAuthService {
    var deviceCodeResponse: MsDeviceCodeResponse? = null
    var deviceCodeError: Throwable? = null

    /** Queue of responses returned on successive pollForToken calls - lets tests simulate "pending" then "success". */
    var pollResponses: MutableList<Response<MsTokenResponse>> = mutableListOf()
    private var pollIndex = 0

    override suspend fun requestDeviceCode(url: String, clientId: String, scope: String): MsDeviceCodeResponse {
        deviceCodeError?.let { throw it }
        return deviceCodeResponse ?: error("FakeMicrosoftAuthService: no deviceCodeResponse configured")
    }

    override suspend fun pollForToken(url: String, clientId: String, deviceCode: String, grantType: String): Response<MsTokenResponse> {
        if (pollResponses.isEmpty()) error("FakeMicrosoftAuthService: no pollResponses configured")
        val response = pollResponses[pollIndex.coerceAtMost(pollResponses.size - 1)]
        if (pollIndex < pollResponses.size - 1) pollIndex++
        return response
    }

    override suspend fun refreshToken(url: String, clientId: String, refreshToken: String, grantType: String, scope: String): MsTokenResponse {
        error("Not used in these tests")
    }
}

class FakeXboxAuthService : XboxAuthService {
    var xboxLiveResponse: XboxLiveAuthResponse? = null
    var xboxLiveError: Throwable? = null
    var xstsResponse: Response<XboxLiveAuthResponse>? = null

    override suspend fun authenticateWithXboxLive(url: String, request: XboxLiveAuthRequest): XboxLiveAuthResponse {
        xboxLiveError?.let { throw it }
        return xboxLiveResponse ?: error("FakeXboxAuthService: no xboxLiveResponse configured")
    }

    override suspend fun authorizeWithXsts(url: String, request: XstsAuthRequest): Response<XboxLiveAuthResponse> {
        return xstsResponse ?: error("FakeXboxAuthService: no xstsResponse configured")
    }
}

class FakeMinecraftAuthService : MinecraftAuthService {
    var loginResponse: MinecraftLoginResponse? = null
    var profileResponse: Response<MinecraftProfileResponse>? = null

    override suspend fun loginWithXbox(request: MinecraftLoginRequest): MinecraftLoginResponse {
        return loginResponse ?: error("FakeMinecraftAuthService: no loginResponse configured")
    }

    override suspend fun getProfile(bearerToken: String): Response<MinecraftProfileResponse> {
        return profileResponse ?: error("FakeMinecraftAuthService: no profileResponse configured")
    }
}

fun <T> successResponse(body: T): Response<T> = Response.success(body)

fun <T> errorResponse(code: Int, message: String = ""): Response<T> =
    Response.error(code, message.toResponseBody("application/json".toMediaTypeOrNull()))
