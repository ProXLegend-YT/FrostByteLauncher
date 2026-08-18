package com.frostbyte.launcher.core.common

/** Mod loader / distribution type, per Section 2 of the PRD. */
enum class Loader {
    VANILLA,
    FABRIC,
    FORGE,
    NEOFORGE,
    QUILT;

    val displayName: String
        get() = when (this) {
            VANILLA -> "Vanilla"
            FABRIC -> "Fabric"
            FORGE -> "Forge"
            NEOFORGE -> "NeoForge"
            QUILT -> "Quilt"
        }
}
