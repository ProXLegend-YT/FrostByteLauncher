package com.frostbyte.launcher.core.network.model

/**
 * Mirrors the per-version JSON Mojang serves at each VersionManifestEntry.url
 * (e.g. https://piston-meta.mojang.com/v1/packages/<sha1>/<version>.json).
 * Only the fields FrostByte actually needs are modeled - Mojang's real JSON
 * has many more fields (arguments, libraries, logging, etc.) that belong to
 * the Java Runtime / Launcher Engine work in Phase 4, not the Version Manager.
 */
data class VersionDetailResponse(
    val id: String,
    val type: String,
    val mainClass: String,
    val downloads: VersionDownloads,
    val javaVersion: JavaVersionRequirement?,
    val assetIndex: AssetIndexRef?,
    val libraries: List<LibraryEntry> = emptyList()
)

data class LibraryEntry(
    val name: String, // Maven coordinate, e.g. "org.lwjgl:lwjgl:3.3.3"
    val downloads: LibraryDownloads?,
    val rules: List<LibraryRule>? = null
)

data class LibraryDownloads(
    val artifact: LibraryArtifact?,
    val classifiers: Map<String, LibraryArtifact>? = null
)

data class LibraryArtifact(
    val path: String,
    val sha1: String,
    val size: Long,
    val url: String
)

data class LibraryRule(
    val action: String, // "allow" | "disallow"
    val os: LibraryOsConstraint?
)

data class LibraryOsConstraint(
    val name: String? // "windows" | "osx" | "linux"
)

data class VersionDownloads(
    val client: DownloadArtifact,
    val server: DownloadArtifact? = null
)

data class DownloadArtifact(
    val sha1: String,
    val size: Long,
    val url: String
)

data class JavaVersionRequirement(
    val component: String,
    val majorVersion: Int
)

data class AssetIndexRef(
    val id: String,
    val sha1: String,
    val size: Long,
    val url: String
)
