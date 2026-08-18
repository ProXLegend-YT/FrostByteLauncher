package com.frostbyte.launcher.core.storage.repository

import com.frostbyte.launcher.core.common.FrostByteResult
import com.frostbyte.launcher.core.network.model.MinecraftVersionType
import com.frostbyte.launcher.core.network.service.MojangMetaService
import com.frostbyte.launcher.core.storage.db.VersionCacheDao
import com.frostbyte.launcher.core.storage.db.VersionCacheEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Client-jar download info for a specific version, resolved on demand (not cached in Room - only fetched right before a download is enqueued). */
data class VersionClientDownload(
    val versionId: String,
    val url: String,
    val sha1: String,
    val sizeBytes: Long
)

/**
 * Fetches the Minecraft version list from Mojang's real API, caching results
 * locally (VersionCacheDao) so the Versions screen still has data offline
 * after a first successful sync. Never fabricates a version list - if the
 * network call fails AND there's no cache yet, callers get a real
 * FrostByteResult.Failure, not a silently empty/fake list.
 */
class VersionRepository(
    private val service: MojangMetaService,
    private val cacheDao: VersionCacheDao
) {
    fun observeCachedVersions(): Flow<List<MinecraftVersion>> =
        cacheDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    /**
     * Refreshes the local cache from the network. Returns Success(count) with
     * how many versions were synced, or Failure with the cache left
     * untouched (so a failed refresh never wipes out previously-cached data
     * a user might still want to browse offline).
     */
    suspend fun refreshFromNetwork(): FrostByteResult<Int> {
        return try {
            val manifest = service.getVersionManifest()
            val now = System.currentTimeMillis()
            val entities = manifest.versions.map { entry ->
                VersionCacheEntity(
                    id = entry.id,
                    type = entry.type,
                    releaseTimeEpochMillis = parseIso8601(entry.releaseTime) ?: now,
                    detailUrl = entry.url,
                    sha1 = entry.sha1,
                    cachedAtEpochMillis = now
                )
            }
            cacheDao.replaceAll(entities)
            FrostByteResult.Success(entities.size)
        } catch (e: IOException) {
            FrostByteResult.Failure("No internet connection - showing cached versions if available", e)
        } catch (e: Exception) {
            FrostByteResult.Failure("Failed to fetch version list from Mojang", e)
        }
    }

    /**
     * Resolves the actual client jar download (URL + SHA-1 + size) for a
     * version by fetching its per-version detail JSON. This is a live
     * network call, not served from cache - the detail JSON is small and
     * fetching it fresh avoids ever handing a downloader a stale/wrong URL.
     */
    suspend fun resolveClientDownload(version: MinecraftVersion): FrostByteResult<VersionClientDownload> {
        return try {
            val detail = service.getVersionDetail(version.detailUrl)
            FrostByteResult.Success(
                VersionClientDownload(
                    versionId = detail.id,
                    url = detail.downloads.client.url,
                    sha1 = detail.downloads.client.sha1,
                    sizeBytes = detail.downloads.client.size
                )
            )
        } catch (e: IOException) {
            FrostByteResult.Failure("No internet connection", e)
        } catch (e: Exception) {
            FrostByteResult.Failure("Failed to resolve download for ${version.id}", e)
        }
    }

    private fun parseIso8601(value: String): Long? {
        // Mojang's releaseTime is ISO-8601 with a timezone offset, e.g.
        // "2024-08-08T12:24:47+00:00". SimpleDateFormat's X pattern (ISO 8601
        // offset) requires API 24+, which is satisfied by this project's
        // min SDK 26.
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            format.parse(value)?.time
        } catch (e: Exception) {
            null
        }
    }
}

private fun VersionCacheEntity.toDomain() = MinecraftVersion(
    id = id,
    type = MinecraftVersionType.fromApiValue(type),
    releaseTimeEpochMillis = releaseTimeEpochMillis,
    detailUrl = detailUrl,
    sha1 = sha1
)
