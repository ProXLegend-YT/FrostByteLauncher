package com.frostbyte.launcher.core.launch

import com.frostbyte.launcher.core.network.model.AssetIndexResponse
import java.io.File

data class ResolvedAsset(
    val virtualPath: String, // e.g. "minecraft/sounds/random/click.ogg"
    val hash: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    /** Where this asset's content-addressed blob lives on disk, e.g. objects/de/de1234...  */
    val objectRelativePath: String
)

/**
 * Resolves a version's asset index (Section 8 of the PRD) into individual
 * downloadable assets, using Mojang's real CDN layout: assets are stored
 * content-addressed by SHA-1 hash at
 * https://resources.download.minecraft.net/<hash[0:2]>/<hash> - NOT at a
 * path resembling the virtual asset name. Getting this sharding wrong is a
 * classic bug in from-scratch launcher implementations, so it's centralized
 * here with a direct test rather than inlined anywhere per-call-site.
 */
object AssetResolver {

    private const val RESOURCES_BASE_URL = "https://resources.download.minecraft.net"

    fun resolveAssets(index: AssetIndexResponse): List<ResolvedAsset> {
        return index.objects.map { (virtualPath, obj) ->
            val shard = obj.hash.take(2)
            ResolvedAsset(
                virtualPath = virtualPath,
                hash = obj.hash,
                sizeBytes = obj.size,
                downloadUrl = "$RESOURCES_BASE_URL/$shard/${obj.hash}",
                objectRelativePath = "objects/$shard/${obj.hash}"
            )
        }
    }

    fun objectFile(assetsDir: File, asset: ResolvedAsset): File =
        File(assetsDir, asset.objectRelativePath)

    /** Total bytes across all assets - useful for showing overall download size before starting. */
    fun totalSizeBytes(assets: List<ResolvedAsset>): Long = assets.sumOf { it.sizeBytes }
}
