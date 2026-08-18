package com.frostbyte.launcher.core.modloader

import com.frostbyte.launcher.core.common.FrostByteResult
import com.frostbyte.launcher.core.network.service.ForgeMetaService
import com.frostbyte.launcher.core.network.service.NeoForgeMetaService
import java.io.IOException

data class ForgeVersionListing(
    val recommended: String?,
    val latest: String?
)

/**
 * Real version listing for Forge (via promotions_slim.json) and NeoForge
 * (via its Maven versions API) - both genuinely completable and tested.
 *
 * Deliberately does NOT expose a resolveInstall() method like
 * FabricQuiltRepository does - see ForgeInstallerRunner's doc comment for
 * why that step is a real, separate, not-yet-built piece of work rather
 * than something safe to fake here.
 */
class ForgeVersionRepository(
    private val forgeMetaService: ForgeMetaService,
    private val neoForgeMetaService: NeoForgeMetaService
) {
    suspend fun getForgeVersions(minecraftVersion: String): FrostByteResult<ForgeVersionListing> {
        return try {
            val promotions = forgeMetaService.getPromotions()
            FrostByteResult.Success(
                ForgeVersionListing(
                    recommended = promotions.promos["$minecraftVersion-recommended"],
                    latest = promotions.promos["$minecraftVersion-latest"]
                )
            )
        } catch (e: IOException) {
            FrostByteResult.Failure("No internet connection", e)
        } catch (e: Exception) {
            FrostByteResult.Failure("Failed to fetch Forge versions", e)
        }
    }

    suspend fun getNeoForgeVersions(minecraftVersion: String): FrostByteResult<List<String>> {
        return try {
            val response = neoForgeMetaService.getVersions()
            // NeoForge version strings encode the target Minecraft version as
            // a prefix, e.g. "21.1.7" targets Minecraft 1.21.1 - matching by
            // that prefix mirrors how NeoForge's own installer/launcher does
            // version discovery.
            val mcVersionPrefix = minecraftVersion.removePrefix("1.")
            val matching = response.versions.filter { it.startsWith(mcVersionPrefix) }
            FrostByteResult.Success(matching)
        } catch (e: IOException) {
            FrostByteResult.Failure("No internet connection", e)
        } catch (e: Exception) {
            FrostByteResult.Failure("Failed to fetch NeoForge versions", e)
        }
    }
}
