package com.frostbyte.launcher.core.runtime

/**
 * A specific, Android/Bionic-compatible JRE build FrostByte knows how to
 * download and run. Deliberately NOT sourced from Mojang's own
 * java-runtime manifest - see the detailed explanation in
 * JavaRuntimeManager.kt for why that manifest's builds don't run on
 * Android.
 *
 * NOTE ON CURRENT STATE: the entries below are placeholders with the
 * correct shape/fields but NOT yet verified, real, current download URLs -
 * populating this catalog requires actually testing candidate JRE builds
 * (e.g. Termux's JDK packages) against a real Android device/emulator to
 * confirm they execute, which isn't possible from this development
 * environment (no Android runtime available here). This is tracked in
 * docs/KNOWN_GAPS.md as a release blocker: shipping with unverified,
 * possibly-broken URLs would fail silently for every user, which is worse
 * than clearly marking the gap.
 */
data class AndroidJreBuild(
    val majorVersion: Int,
    val abi: String, // "arm64-v8a" | "armeabi-v7a" | "x86_64"
    val archiveUrl: String,
    val archiveSha256: String,
    val archiveSizeBytes: Long
)

object JavaRuntimeCatalog {

    /**
     * Placeholder catalog - see class-level doc. Entries intentionally use
     * an obviously-fake sha256/size (all zeros / -1) rather than a
     * plausible-looking fake, so a real verification step (checksum
     * mismatch) fails loudly instead of a wrong hash silently "passing" in
     * some future refactor.
     */
    private val builds = listOf(
        AndroidJreBuild(
            majorVersion = 17,
            abi = "arm64-v8a",
            archiveUrl = "", // TODO(release-blocker): populate with a verified URL - see docs/KNOWN_GAPS.md
            archiveSha256 = "0".repeat(64),
            archiveSizeBytes = -1L
        ),
        AndroidJreBuild(
            majorVersion = 21,
            abi = "arm64-v8a",
            archiveUrl = "", // TODO(release-blocker): populate with a verified URL - see docs/KNOWN_GAPS.md
            archiveSha256 = "0".repeat(64),
            archiveSizeBytes = -1L
        )
    )

    /** Returns null if no known build exists for this (version, abi) pair - callers must handle this as a real "unsupported" state, not fall back to guessing. */
    fun findBuild(majorVersion: Int, abi: String): AndroidJreBuild? =
        builds.firstOrNull { it.majorVersion == majorVersion && it.abi == abi && it.archiveUrl.isNotBlank() }
}
