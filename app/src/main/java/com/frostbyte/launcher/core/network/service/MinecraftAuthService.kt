package com.frostbyte.launcher.core.network.service

import com.frostbyte.launcher.core.network.model.MinecraftLoginRequest
import com.frostbyte.launcher.core.network.model.MinecraftLoginResponse
import com.frostbyte.launcher.core.network.model.MinecraftProfileResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface MinecraftAuthService {

    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST(MinecraftServicesUrls.MINECRAFT_LOGIN_PATH)
    suspend fun loginWithXbox(@Body request: MinecraftLoginRequest): MinecraftLoginResponse

    /**
     * Returns a 404 when the account genuinely does not own Minecraft -
     * this is Mojang's real, documented ownership-check behavior, not a
     * bug. AuthRepository surfaces this as an honest "this account doesn't
     * own Minecraft" message rather than a generic network error, and never
     * falls back to pretending ownership.
     */
    @GET(MinecraftServicesUrls.MINECRAFT_PROFILE_PATH)
    suspend fun getProfile(@Header("Authorization") bearerToken: String): Response<MinecraftProfileResponse>
}

/** Path constants split out so MinecraftAuthService can use relative @POST/@GET (needs a Retrofit baseUrl), unlike the other auth services which use absolute @Url. */
object MinecraftServicesUrls {
    const val BASE_URL = "https://api.minecraftservices.com/"
    const val MINECRAFT_LOGIN_PATH = "authentication/login_with_xbox"
    const val MINECRAFT_PROFILE_PATH = "minecraft/profile"
}
