package com.frostbyte.launcher.core.network

import com.frostbyte.launcher.core.network.service.FabricMetaService
import com.frostbyte.launcher.core.network.service.ForgeMetaService
import com.frostbyte.launcher.core.network.service.MicrosoftAuthService
import com.frostbyte.launcher.core.network.service.MinecraftAuthService
import com.frostbyte.launcher.core.network.service.MinecraftServicesUrls
import com.frostbyte.launcher.core.network.service.ModrinthService
import com.frostbyte.launcher.core.network.service.MojangMetaService
import com.frostbyte.launcher.core.network.service.NeoForgeMetaService
import com.frostbyte.launcher.core.network.service.QuiltMetaService
import com.frostbyte.launcher.core.network.service.XboxAuthService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the shared OkHttp client and Retrofit services. Kept as plain
 * factory functions rather than a DI-framework module, consistent with the
 * manual FrostByteContainer approach from Phase 2 - Hilt is still deferred
 * until the dependency graph actually needs it.
 */
object NetworkModule {

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val mojangMetaService: MojangMetaService by lazy {
        Retrofit.Builder()
            .baseUrl(MojangMetaService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MojangMetaService::class.java)
    }

    // Microsoft and Xbox auth services only ever call absolute @Url
    // endpoints, so their baseUrl is never actually used - it's set to
    // Microsoft's own domain anyway, since Retrofit requires *a* valid
    // baseUrl to be configured regardless.
    private val microsoftLoginRetrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://login.microsoftonline.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val microsoftAuthService: MicrosoftAuthService by lazy {
        microsoftLoginRetrofit.create(MicrosoftAuthService::class.java)
    }

    val xboxAuthService: XboxAuthService by lazy {
        microsoftLoginRetrofit.create(XboxAuthService::class.java)
    }

    val minecraftAuthService: MinecraftAuthService by lazy {
        Retrofit.Builder()
            .baseUrl(MinecraftServicesUrls.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MinecraftAuthService::class.java)
    }

    val fabricMetaService: FabricMetaService by lazy {
        Retrofit.Builder()
            .baseUrl(FabricMetaService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(FabricMetaService::class.java)
    }

    val quiltMetaService: QuiltMetaService by lazy {
        Retrofit.Builder()
            .baseUrl(QuiltMetaService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QuiltMetaService::class.java)
    }

    val forgeMetaService: ForgeMetaService by lazy {
        Retrofit.Builder()
            .baseUrl(ForgeMetaService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ForgeMetaService::class.java)
    }

    val neoForgeMetaService: NeoForgeMetaService by lazy {
        Retrofit.Builder()
            .baseUrl(NeoForgeMetaService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NeoForgeMetaService::class.java)
    }

    val modrinthService: ModrinthService by lazy {
        Retrofit.Builder()
            .baseUrl(ModrinthService.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ModrinthService::class.java)
    }
}
