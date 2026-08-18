package com.frostbyte.launcher.core.network.model

import com.google.gson.annotations.SerializedName

/**
 * Mirrors Mojang's official version_manifest_v2.json shape at
 * https://piston-meta.mojang.com/mc/game/version_manifest_v2.json
 *
 * Field names match the real API exactly (snake_case where Mojang uses it)
 * since this is a straight JSON mapping, not a redesigned model.
 */
data class VersionManifestResponse(
    val latest: LatestVersions,
    val versions: List<VersionManifestEntry>
)

data class LatestVersions(
    val release: String,
    val snapshot: String
)

data class VersionManifestEntry(
    val id: String,
    val type: String, // "release" | "snapshot" | "old_beta" | "old_alpha"
    val url: String,
    val time: String,
    @SerializedName("releaseTime") val releaseTime: String,
    val sha1: String
)

/** Parsed, app-friendly version of [VersionManifestEntry.type]. */
enum class MinecraftVersionType {
    RELEASE, SNAPSHOT, OLD_BETA, OLD_ALPHA, UNKNOWN;

    companion object {
        fun fromApiValue(value: String): MinecraftVersionType = when (value) {
            "release" -> RELEASE
            "snapshot" -> SNAPSHOT
            "old_beta" -> OLD_BETA
            "old_alpha" -> OLD_ALPHA
            else -> UNKNOWN
        }
    }
}
