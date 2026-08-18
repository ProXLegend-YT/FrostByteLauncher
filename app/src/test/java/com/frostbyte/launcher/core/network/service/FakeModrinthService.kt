package com.frostbyte.launcher.core.network.service

import com.frostbyte.launcher.core.network.model.ModrinthProjectResponse
import com.frostbyte.launcher.core.network.model.ModrinthSearchResponse
import com.frostbyte.launcher.core.network.model.ModrinthVersionResponse

class FakeModrinthService : ModrinthService {
    var searchResponse: ModrinthSearchResponse? = null
    var projectResponse: ModrinthProjectResponse? = null
    var versionsResponse: List<ModrinthVersionResponse>? = null
    var error: Throwable? = null

    /** Records the last facets string passed to search(), so tests can assert on it. */
    var lastFacets: String? = null
        private set

    override suspend fun search(query: String, facets: String?, limit: Int, offset: Int): ModrinthSearchResponse {
        lastFacets = facets
        error?.let { throw it }
        return searchResponse ?: error("FakeModrinthService: no searchResponse configured")
    }

    override suspend fun getProject(idOrSlug: String): ModrinthProjectResponse {
        error?.let { throw it }
        return projectResponse ?: error("FakeModrinthService: no projectResponse configured")
    }

    override suspend fun getVersions(idOrSlug: String, gameVersions: String?, loaders: String?): List<ModrinthVersionResponse> {
        error?.let { throw it }
        return versionsResponse ?: error("FakeModrinthService: no versionsResponse configured")
    }
}
