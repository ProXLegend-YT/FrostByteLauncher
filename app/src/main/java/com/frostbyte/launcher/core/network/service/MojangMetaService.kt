package com.frostbyte.launcher.core.network.service

import com.frostbyte.launcher.core.network.model.VersionDetailResponse
import com.frostbyte.launcher.core.network.model.VersionManifestResponse
import retrofit2.http.GET
import retrofit2.http.Url

/**
 * Mojang's official, publicly documented version metadata API. FrostByte
 * only ever reads version *metadata* from Mojang here - it never proxies,
 * caches, or redistributes actual game asset/jar bytes through any
 * FrostByte-controlled server (there is no FrostByte backend at all).
 */
interface MojangMetaService {

    @GET("mc/game/version_manifest_v2.json")
    suspend fun getVersionManifest(): VersionManifestResponse

    /**
     * Each VersionManifestEntry.url is a full absolute URL (not a relative
     * path under this service's base URL), so this uses @Url to fetch it
     * verbatim rather than trying to force it through a templated path.
     */
    @GET
    suspend fun getVersionDetail(@Url url: String): VersionDetailResponse

    companion object {
        const val BASE_URL = "https://piston-meta.mojang.com/"
    }
}
