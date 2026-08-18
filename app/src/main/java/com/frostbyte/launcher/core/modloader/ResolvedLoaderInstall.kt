package com.frostbyte.launcher.core.modloader

import com.frostbyte.launcher.core.common.Loader
import com.frostbyte.launcher.core.network.model.LibraryEntry

data class LoaderVersion(
    val version: String,
    val stable: Boolean
)

/**
 * A fully resolved loader install, ready to be merged with the vanilla
 * version's own libraries/classpath by LaunchPreparer (Phase 4). mainClass
 * here REPLACES the vanilla main class (e.g. Fabric's knot client launcher),
 * and libraries are ADDITIONAL entries appended to the vanilla classpath.
 */
data class ResolvedLoaderInstall(
    val loader: Loader,
    val loaderVersion: String,
    val minecraftVersion: String,
    val mainClass: String,
    val additionalLibraries: List<LibraryEntry>
)
