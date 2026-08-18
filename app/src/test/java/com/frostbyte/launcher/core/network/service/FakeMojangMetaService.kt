package com.frostbyte.launcher.core.network.service

import com.frostbyte.launcher.core.network.model.VersionDetailResponse
import com.frostbyte.launcher.core.network.model.VersionManifestResponse

/**
 * Scriptable fake of MojangMetaService for JVM unit tests. Each response can
 * be set directly, or an exception can be queued to simulate a network
 * failure, without needing MockWebServer for tests that only care about
 * repository/ViewModel-level behavior (not actual HTTP parsing - that's
 * covered separately where relevant).
 */
class FakeMojangMetaService : MojangMetaService {
    var manifestResponse: VersionManifestResponse? = null
    var manifestError: Throwable? = null

    var detailResponsesByUrl: MutableMap<String, VersionDetailResponse> = mutableMapOf()
    var detailError: Throwable? = null

    override suspend fun getVersionManifest(): VersionManifestResponse {
        manifestError?.let { throw it }
        return manifestResponse ?: error("FakeMojangMetaService: no manifestResponse configured")
    }

    override suspend fun getVersionDetail(url: String): VersionDetailResponse {
        detailError?.let { throw it }
        return detailResponsesByUrl[url] ?: error("FakeMojangMetaService: no detail configured for $url")
    }
}
