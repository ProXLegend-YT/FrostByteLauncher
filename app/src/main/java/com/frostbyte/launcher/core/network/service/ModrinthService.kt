package com.frostbyte.launcher.core.network.service

import com.frostbyte.launcher.core.network.model.ModrinthProjectResponse
import com.frostbyte.launcher.core.network.model.ModrinthSearchResponse
import com.frostbyte.launcher.core.network.model.ModrinthVersionResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ModrinthService {

    @GET("v2/search")
    suspend fun search(
        @Query("query") query: String,
        @Query("facets") facets: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): ModrinthSearchResponse

    @GET("v2/project/{idOrSlug}")
    suspend fun getProject(@Path("idOrSlug") idOrSlug: String): ModrinthProjectResponse

    @GET("v2/project/{idOrSlug}/version")
    suspend fun getVersions(
        @Path("idOrSlug") idOrSlug: String,
        @Query("game_versions") gameVersions: String? = null, // JSON array string, e.g. ["1.21.1"]
        @Query("loaders") loaders: String? = null // JSON array string, e.g. ["fabric"]
    ): List<ModrinthVersionResponse>

    companion object {
        const val BASE_URL = "https://api.modrinth.com/"
    }
}

/**
 * Builds Modrinth's facets query parameter - a JSON string of the form
 * `[["project_type:mod"],["versions:1.21.1"]]`, where each inner array is
 * OR'd together and outer arrays are AND'd. Getting this wrong silently
 * returns unfiltered (or wrongly filtered) results rather than an error, so
 * it's centralized here with direct tests rather than hand-built per call
 * site.
 */
object ModrinthFacetsBuilder {
    fun build(projectType: String? = null, minecraftVersion: String? = null, loader: String? = null): String? {
        val groups = mutableListOf<List<String>>()
        projectType?.let { groups += listOf("project_type:$it") }
        minecraftVersion?.let { groups += listOf("versions:$it") }
        loader?.let { groups += listOf("categories:$it") }
        if (groups.isEmpty()) return null

        return groups.joinToString(prefix = "[", postfix = "]", separator = ",") { group ->
            group.joinToString(prefix = "[", postfix = "]", separator = ",") { "\"$it\"" }
        }
    }
}
