package com.frostbyte.launcher.core.network.model

/**
 * Forge and NeoForge do NOT have a Fabric-style clean JSON meta API for
 * "give me a launch profile." The real, honest picture:
 *
 * - Version listing: Forge publishes a promotions_slim.json at
 *   https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json
 *   mapping Minecraft versions to recommended/latest Forge build numbers.
 *   NeoForge has an equivalent at
 *   https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge
 * - Installation: each Forge/NeoForge version is distributed as an
 *   "installer" jar hosted on their Maven repositories. The REAL, official
 *   way to install is to run that installer jar's own client-install logic
 *   (it contains an install_profile.json describing exactly what libraries
 *   to fetch and how to patch the vanilla jar) - not to guess at a
 *   simplified library list by hand, which would silently produce a broken
 *   install for many versions (Forge's patching process varies significantly
 *   across Minecraft version eras).
 *
 * ForgePromotionsResponse models the version-listing step, which IS clean
 * JSON and safe to model directly. The installer-execution step is
 * intentionally NOT modeled as a simple data class here - see
 * ForgeInstallerRunner's doc comment for why, and docs/KNOWN_GAPS.md for
 * what remains to build.
 */
data class ForgePromotionsResponse(
    val promos: Map<String, String> // e.g. "1.21.1-recommended" -> "52.0.1", "1.21.1-latest" -> "52.0.2"
)

data class NeoForgeVersionsResponse(
    val isSnapshot: Boolean,
    val versions: List<String>
)
