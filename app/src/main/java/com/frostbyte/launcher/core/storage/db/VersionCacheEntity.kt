package com.frostbyte.launcher.core.storage.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local cache of Mojang's version manifest, so the Versions screen has
 * something to show offline / on a flaky connection after the first
 * successful fetch, rather than going blank. This is a cache, not a source
 * of truth - VersionRepository always tries the network first and only
 * falls back to this table on failure.
 */
@Entity(tableName = "version_cache")
data class VersionCacheEntity(
    @PrimaryKey val id: String, // Minecraft version id, e.g. "1.21.1"
    val type: String,
    val releaseTimeEpochMillis: Long,
    val detailUrl: String,
    val sha1: String,
    val cachedAtEpochMillis: Long
)
