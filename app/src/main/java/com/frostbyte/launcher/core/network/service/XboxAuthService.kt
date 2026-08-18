package com.frostbyte.launcher.core.network.service

import com.frostbyte.launcher.core.network.model.XboxLiveAuthRequest
import com.frostbyte.launcher.core.network.model.XboxLiveAuthResponse
import com.frostbyte.launcher.core.network.model.XstsAuthRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Url

interface XboxAuthService {

    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST
    suspend fun authenticateWithXboxLive(
        @Url url: String,
        @Body request: XboxLiveAuthRequest
    ): XboxLiveAuthResponse

    /**
     * XSTS authorization can fail with a specific, documented set of error
     * codes (e.g. XErr 2148916233 = "no Xbox account", 2148916238 = "child
     * account needs adult verification") - returning the raw Response lets
     * AuthRepository surface Mojang's actual documented meaning for these
     * instead of a generic HTTP failure.
     */
    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST
    suspend fun authorizeWithXsts(
        @Url url: String,
        @Body request: XstsAuthRequest
    ): Response<XboxLiveAuthResponse>
}
