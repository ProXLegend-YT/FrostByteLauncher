package com.frostbyte.launcher.core.modloader

/**
 * ============================================================================
 * NOT YET IMPLEMENTED - read before assuming Forge/NeoForge installs work.
 * ============================================================================
 *
 * Forge and NeoForge distribute each version as a self-contained "installer"
 * jar (see ForgeMetaService.installerJarPath / NeoForgeMetaService.installerJarPath).
 * That installer jar contains an install_profile.json plus a version JSON
 * describing exactly which libraries to fetch and, critically, HOW to patch
 * the vanilla client jar (Forge/NeoForge inject transformed bytecode into
 * vanilla classes - this is fundamentally different from Fabric/Quilt, which
 * only ADD libraries and swap the main class without touching vanilla code).
 *
 * The real, correct way to install Forge/NeoForge is one of:
 *   (a) Actually execute the installer jar's own client-install logic (it's
 *       a runnable jar with a real main class for this), OR
 *   (b) Faithfully reimplement that logic: parse install_profile.json,
 *       resolve its own library list, and run its "processors" - the actual
 *       jar-patching steps it defines, in order, with their own arguments.
 *
 * Both are real, substantial engineering work - patching logic has changed
 * meaningfully across Minecraft/Forge version eras, and getting it wrong
 * doesn't fail loudly, it produces a jar that crashes at runtime in
 * confusing ways. Attempting a "simplified" version here would violate the
 * same principle that governs the rest of this codebase (Section 11 of the
 * PRD: don't fake success) - a Forge install that silently skips real
 * patching steps is worse than no Forge support at all, because it would
 * look installed while being broken.
 *
 * What FrostByte has today for Forge/NeoForge (Phase 6, partial):
 *   - Real version listing (ForgeVersionRepository) - genuinely complete
 *   - NO installer execution - this class is the explicit marker for that
 *
 * FabricQuiltRepository-style resolveInstall() is intentionally absent for
 * Forge/NeoForge (see ForgeVersionRepository's doc comment) rather than
 * present with fake/simplified behavior.
 */
object ForgeInstallerRunner {
    fun isImplemented(): Boolean = false
}
