package com.frostbyte.launcher.core.network.model

/**
 * Mirrors the asset index JSON a VersionDetailResponse.assetIndex.url points
 * to - a flat map of virtual asset paths (e.g. "minecraft/sounds/...") to
 * their content hash and size. Real assets are fetched individually from
 * Mojang's resources CDN at a URL derived from the hash (see AssetResolver).
 */
data class AssetIndexResponse(
    val objects: Map<String, AssetObject>
)

data class AssetObject(
    val hash: String,
    val size: Long
)
