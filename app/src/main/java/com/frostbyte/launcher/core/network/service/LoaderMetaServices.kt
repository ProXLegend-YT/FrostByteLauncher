package com.frostbyte.launcher.core.network.service

import com.frostbyte.launcher.core.network.model.LoaderProfileResponse
import com.frostbyte.launcher.core.network.model.LoaderVersionEntry
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Fabric's real, public meta API - https://meta.fabricmc.net.
 * Documented at https://github.com/FabricMC/fabric-meta.
 */
interface FabricMetaService {
    @GET("v2/versions/loader/{mcVersion}")
    suspend fun getLoaderVersions(@Path("mcVersion") minecraftVersion: String): List<LoaderVersionEntry>

    @GET("v2/versions/loader/{mcVersion}/{loaderVersion}/profile/json")
    suspend fun getLoaderProfile(
        @Path("mcVersion") minecraftVersion: String,
        @Path("loaderVersion") loaderVersion: String
    ): LoaderProfileResponse

    companion object {
        const val BASE_URL = "https://meta.fabricmc.net/"
    }
}

/**
 * Quilt's meta API - https://meta.quiltmc.org. Quilt is a Fabric fork that
 * deliberately kept its meta API shape compatible with Fabric's, so this
 * reuses the exact same response models (LoaderVersionEntry,
 * LoaderProfileResponse) against a different base URL and version prefix.
 */
interface QuiltMetaService {
    @GET("v3/versions/loader/{mcVersion}")
    suspend fun getLoaderVersions(@Path("mcVersion") minecraftVersion: String): List<LoaderVersionEntry>

    @GET("v3/versions/loader/{mcVersion}/{loaderVersion}/profile/json")
    suspend fun getLoaderProfile(
        @Path("mcVersion") minecraftVersion: String,
        @Path("loaderVersion") loaderVersion: String
    ): LoaderProfileResponse

    companion object {
        const val BASE_URL = "https://meta.quiltmc.org/"
    }
}
