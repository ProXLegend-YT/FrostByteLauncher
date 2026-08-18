package com.frostbyte.launcher.core.modloader

import com.frostbyte.launcher.core.common.FrostByteResult
import com.frostbyte.launcher.core.common.Loader
import com.frostbyte.launcher.core.network.service.FabricMetaService
import com.frostbyte.launcher.core.network.service.QuiltMetaService
import java.io.IOException

/**
 * Real, complete Fabric/Quilt loader resolution - unlike Forge/NeoForge
 * (see ForgeInstallerRunner), both of these expose a genuine JSON meta API
 * that directly returns everything needed for a launch profile, so there is
 * no "installer execution" step to fake or stub here.
 */
class FabricQuiltRepository(
    private val fabricMetaService: FabricMetaService,
    private val quiltMetaService: QuiltMetaService
) {
    suspend fun getAvailableVersions(loader: Loader, minecraftVersion: String): FrostByteResult<List<LoaderVersion>> {
        require(loader == Loader.FABRIC || loader == Loader.QUILT) { "getAvailableVersions only supports Fabric/Quilt, got $loader" }
        return try {
            val entries = when (loader) {
                Loader.FABRIC -> fabricMetaService.getLoaderVersions(minecraftVersion)
                Loader.QUILT -> quiltMetaService.getLoaderVersions(minecraftVersion)
                else -> error("unreachable")
            }
            FrostByteResult.Success(entries.map { LoaderVersion(version = it.loader.version, stable = it.loader.stable) })
        } catch (e: IOException) {
            FrostByteResult.Failure("No internet connection", e)
        } catch (e: Exception) {
            FrostByteResult.Failure("Failed to fetch ${loader.displayName} versions for $minecraftVersion", e)
        }
    }

    suspend fun resolveInstall(loader: Loader, minecraftVersion: String, loaderVersion: String): FrostByteResult<ResolvedLoaderInstall> {
        require(loader == Loader.FABRIC || loader == Loader.QUILT) { "resolveInstall only supports Fabric/Quilt, got $loader" }
        return try {
            val profile = when (loader) {
                Loader.FABRIC -> fabricMetaService.getLoaderProfile(minecraftVersion, loaderVersion)
                Loader.QUILT -> quiltMetaService.getLoaderProfile(minecraftVersion, loaderVersion)
                else -> error("unreachable")
            }
            FrostByteResult.Success(
                ResolvedLoaderInstall(
                    loader = loader,
                    loaderVersion = loaderVersion,
                    minecraftVersion = minecraftVersion,
                    mainClass = profile.mainClass,
                    additionalLibraries = profile.libraries
                )
            )
        } catch (e: IOException) {
            FrostByteResult.Failure("No internet connection", e)
        } catch (e: Exception) {
            FrostByteResult.Failure("Failed to resolve ${loader.displayName} $loaderVersion for $minecraftVersion", e)
        }
    }
}
