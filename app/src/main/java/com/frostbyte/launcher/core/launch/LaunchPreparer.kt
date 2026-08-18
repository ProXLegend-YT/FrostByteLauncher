package com.frostbyte.launcher.core.launch

import com.frostbyte.launcher.core.network.model.VersionDetailResponse
import com.frostbyte.launcher.core.runtime.JavaRuntimeManager
import java.io.File

sealed class LaunchPreparationResult {
    data class Ready(val config: LaunchConfig) : LaunchPreparationResult()
    data class MissingJavaRuntime(val requiredMajorVersion: Int) : LaunchPreparationResult()
    data class MissingFiles(val description: String) : LaunchPreparationResult()
}

data class LaunchIdentity(
    val username: String,
    val uuid: String,
    val accessToken: String
)

/**
 * Ties together LibraryResolver and AssetResolver into one "given a
 * version's detail JSON and where things live on disk, build the
 * LaunchConfig LauncherEngine needs" step. This is the orchestration layer -
 * it does not itself download anything (that's the Download Manager's job,
 * Phase 3) or extract natives (that already needs to have happened via
 * NativesResolver.extract beforehand); it only checks that everything
 * required is ALREADY present and builds the resulting config, or reports
 * precisely what's missing so the caller can trigger the right download.
 */
class LaunchPreparer(
    private val javaRuntimeManager: JavaRuntimeManager
) {
    fun prepare(
        versionDetail: VersionDetailResponse,
        librariesDir: File,
        nativesDir: File,
        assetsDir: File,
        gameDirectory: File,
        ramAllocationMb: Int,
        extraJvmArguments: List<String>,
        identity: LaunchIdentity,
        windowWidth: Int? = null,
        windowHeight: Int? = null
    ): LaunchPreparationResult {
        val requiredJavaMajor = versionDetail.javaVersion?.majorVersion ?: 17
        val javaBinary = javaRuntimeManager.installedJavaBinary(requiredJavaMajor)
            ?: return LaunchPreparationResult.MissingJavaRuntime(requiredJavaMajor)

        val resolvedLibraries = LibraryResolver.resolveClasspathLibraries(versionDetail.libraries)
        val classpathFiles = LibraryResolver.resolveClasspathFiles(librariesDir, resolvedLibraries)
        val missingLibraries = classpathFiles.filterNot { it.exists() }
        if (missingLibraries.isNotEmpty()) {
            return LaunchPreparationResult.MissingFiles(
                "${missingLibraries.size} required library file(s) not downloaded yet, e.g. ${missingLibraries.first().name}"
            )
        }

        val assetIndexId = versionDetail.assetIndex?.id
            ?: return LaunchPreparationResult.MissingFiles("Version detail has no asset index")

        val config = LaunchConfig(
            javaBinary = javaBinary,
            ramAllocationMb = ramAllocationMb,
            extraJvmArguments = extraJvmArguments,
            mainClass = versionDetail.mainClass,
            classpathEntries = classpathFiles,
            nativesDirectory = nativesDir,
            gameDirectory = gameDirectory,
            assetsDirectory = assetsDir,
            assetIndexId = assetIndexId,
            minecraftVersion = versionDetail.id,
            username = identity.username,
            uuid = identity.uuid,
            accessToken = identity.accessToken,
            windowWidth = windowWidth,
            windowHeight = windowHeight
        )
        return LaunchPreparationResult.Ready(config)
    }
}
