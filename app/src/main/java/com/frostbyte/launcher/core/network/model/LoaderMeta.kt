package com.frostbyte.launcher.core.network.model

/**
 * Fabric and Quilt share an API-compatible meta server shape (Quilt is a
 * Fabric fork and deliberately kept its meta API compatible), so both use
 * these same models against different base URLs - see FabricMetaService
 * and QuiltMetaService.
 */

data class LoaderVersionEntry(
    val loader: LoaderVersionInfo
)

data class LoaderVersionInfo(
    val separator: String,
    val build: Int,
    val maven: String, // Maven coordinate, e.g. "net.fabricmc:fabric-loader:0.16.9"
    val version: String,
    val stable: Boolean
)

/**
 * The full launch profile for a given (Minecraft version, loader version)
 * pair - this is what actually gets merged with the vanilla version detail
 * to produce a moddable launch (extra libraries + a different main class).
 */
data class LoaderProfileResponse(
    val id: String,
    val inheritsFrom: String, // the vanilla Minecraft version this profile extends
    val mainClass: String,
    val libraries: List<LibraryEntry>
)
