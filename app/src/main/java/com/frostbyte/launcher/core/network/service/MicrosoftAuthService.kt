package com.frostbyte.launcher.core.network.service

import com.frostbyte.launcher.core.network.model.MsDeviceCodeResponse
import com.frostbyte.launcher.core.network.model.MsTokenResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import retrofit2.http.Url

interface MicrosoftAuthService {

    @FormUrlEncoded
    @POST
    suspend fun requestDeviceCode(
        @Url url: String,
        @Field("client_id") clientId: String,
        @Field("scope") scope: String
    ): MsDeviceCodeResponse

    /**
     * Polls for a completed device-code sign-in. Returns the raw
     * retrofit2.Response rather than throwing, because "authorization
     * pending" (HTTP 400 with error=authorization_pending) is an EXPECTED,
     * routine response while the user hasn't finished signing in yet in
     * their browser - it is not an error condition worth logging/retrying
     * as a failure, and callers need the raw status + body to distinguish
     * "still waiting" from "actually failed."
     */
    @FormUrlEncoded
    @POST
    suspend fun pollForToken(
        @Url url: String,
        @Field("client_id") clientId: String,
        @Field("device_code") deviceCode: String,
        @Field("grant_type") grantType: String = "urn:ietf:params:oauth:grant-type:device_code"
    ): Response<MsTokenResponse>

    @FormUrlEncoded
    @POST
    suspend fun refreshToken(
        @Url url: String,
        @Field("client_id") clientId: String,
        @Field("refresh_token") refreshToken: String,
        @Field("grant_type") grantType: String = "refresh_token",
        @Field("scope") scope: String
    ): MsTokenResponse
}
