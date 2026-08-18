package com.frostbyte.launcher.core.network.model

/**
 * Mirrors Modrinth's real, public, no-API-key-required v2 API
 * (https://docs.modrinth.com/api). Covers search, project detail, and
 * version listing - enough to discover and install mods/shaders/resource
 * packs (Section 10-12 of the PRD).
 */

data class ModrinthSearchResponse(
    val hits: List<ModrinthSearchHit>,
    val total_hits: Int
)

data class ModrinthSearchHit(
    val project_id: String,
    val slug: String,
    val title: String,
    val description: String,
    val icon_url: String?,
    val downloads: Int,
    val project_type: String, // "mod" | "shader" | "resourcepack"
    val categories: List<String>
)

data class ModrinthProjectResponse(
    val id: String,
    val slug: String,
    val title: String,
    val description: String,
    val body: String,
    val icon_url: String?,
    val downloads: Int,
    val project_type: String
)

data class ModrinthVersionResponse(
    val id: String,
    val project_id: String,
    val name: String,
    val version_number: String,
    val game_versions: List<String>,
    val loaders: List<String>,
    val files: List<ModrinthVersionFile>
)

data class ModrinthVersionFile(
    val url: String,
    val filename: String,
    val primary: Boolean,
    val size: Long,
    val hashes: ModrinthFileHashes
)

data class ModrinthFileHashes(
    val sha1: String,
    val sha512: String
)
