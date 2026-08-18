package com.frostbyte.launcher.core.content

import com.frostbyte.launcher.core.common.FrostByteResult
import com.frostbyte.launcher.core.network.model.ModrinthSearchHit
import com.frostbyte.launcher.core.network.service.ModrinthFacetsBuilder
import com.frostbyte.launcher.core.network.service.ModrinthService
import java.io.IOException

/**
 * Real content search/resolution against Modrinth's genuine public API -
 * Section 10-12 of the PRD (Mods, Shaders, Resource Packs). No API key is
 * required for Modrinth, unlike CurseForge (see CurseForgeConfig).
 */
class ModrinthContentRepository(private val service: ModrinthService) {

    suspend fun search(
        query: String,
        contentType: ContentType,
        minecraftVersion: String? = null,
        loader: String? = null
    ): FrostByteResult<List<ContentSearchResult>> {
        return try {
            val facets = ModrinthFacetsBuilder.build(
                projectType = contentType.modrinthProjectType,
                minecraftVersion = minecraftVersion,
                loader = loader
            )
            val response = service.search(query = query, facets = facets)
            FrostByteResult.Success(response.hits.map { it.toDomain() })
        } catch (e: IOException) {
            FrostByteResult.Failure("No internet connection", e)
        } catch (e: Exception) {
            FrostByteResult.Failure("Search failed: ${e.message ?: e::class.simpleName}", e)
        }
    }

    suspend fun getVersions(
        projectIdOrSlug: String,
        minecraftVersion: String? = null,
        loader: String? = null
    ): FrostByteResult<List<ContentVersion>> {
        return try {
            val gameVersionsParam = minecraftVersion?.let { """["$it"]""" }
            val loadersParam = loader?.let { """["$it"]""" }
            val versions = service.getVersions(projectIdOrSlug, gameVersionsParam, loadersParam)

            val resolved = versions.mapNotNull { version ->
                val primaryFile = version.files.firstOrNull { it.primary } ?: version.files.firstOrNull()
                primaryFile?.let {
                    ContentVersion(
                        id = version.id,
                        versionNumber = version.version_number,
                        gameVersions = version.game_versions,
                        loaders = version.loaders,
                        fileUrl = it.url,
                        filename = it.filename,
                        sizeBytes = it.size,
                        sha1 = it.hashes.sha1
                    )
                }
            }
            FrostByteResult.Success(resolved)
        } catch (e: IOException) {
            FrostByteResult.Failure("No internet connection", e)
        } catch (e: Exception) {
            FrostByteResult.Failure("Failed to fetch versions for $projectIdOrSlug", e)
        }
    }
}

private fun ModrinthSearchHit.toDomain() = ContentSearchResult(
    id = project_id,
    slug = slug,
    title = title,
    description = description,
    iconUrl = icon_url,
    downloadCount = downloads,
    contentType = when (project_type) {
        "mod" -> ContentType.MOD
        "shader" -> ContentType.SHADER
        "resourcepack" -> ContentType.RESOURCE_PACK
        else -> ContentType.MOD // unknown types default to MOD rather than crashing - Modrinth occasionally adds new project types (e.g. "plugin", "datapack") this app doesn't specifically support yet
    }
)
