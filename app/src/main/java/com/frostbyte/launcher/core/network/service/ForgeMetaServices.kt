package com.frostbyte.launcher.core.network.service

import com.frostbyte.launcher.core.network.model.ForgePromotionsResponse
import com.frostbyte.launcher.core.network.model.NeoForgeVersionsResponse
import retrofit2.http.GET

interface ForgeMetaService {
    @GET("net/minecraftforge/forge/promotions_slim.json")
    suspend fun getPromotions(): ForgePromotionsResponse

    companion object {
        const val BASE_URL = "https://files.minecraftforge.net/"
        /** Maven path to a given Forge version's installer jar - used by ForgeInstallerRunner, not fetched via this Retrofit service. */
        fun installerJarPath(minecraftVersion: String, forgeVersion: String) =
            "https://maven.minecraftforge.net/net/minecraftforge/forge/$minecraftVersion-$forgeVersion/forge-$minecraftVersion-$forgeVersion-installer.jar"
    }
}

interface NeoForgeMetaService {
    @GET("api/maven/versions/releases/net/neoforged/neoforge")
    suspend fun getVersions(): NeoForgeVersionsResponse

    companion object {
        const val BASE_URL = "https://maven.neoforged.net/"
        fun installerJarPath(neoForgeVersion: String) =
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/$neoForgeVersion/neoforge-$neoForgeVersion-installer.jar"
    }
}
