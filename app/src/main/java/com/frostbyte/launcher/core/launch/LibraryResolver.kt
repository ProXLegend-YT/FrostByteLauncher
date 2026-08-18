package com.frostbyte.launcher.core.launch

import com.frostbyte.launcher.core.network.model.LibraryEntry
import java.io.File

data class ResolvedLibrary(
    val mavenName: String,
    val relativePath: String,
    val sha1: String,
    val sizeBytes: Long,
    val downloadUrl: String
)

/**
 * Resolves a version's `libraries` array (Section 8 of the PRD) into the
 * concrete set of jars that belong on this platform's classpath, applying
 * the same OS-rule filtering Mojang's own launcher uses (a library entry
 * can be restricted to windows/osx/linux only, or explicitly disallowed on
 * one).
 *
 * FrostByte always resolves against "linux" rules, since Android's kernel
 * is Linux - this is specifically about which JAR variants to download
 * (e.g. platform-specific LWJGL natives), which is a real and correct use
 * of the Linux OS rule, separate from (and not to be confused with) the JRE
 * binary-compatibility issue documented in JavaRuntimeManager.kt.
 */
object LibraryResolver {

    private const val TARGET_OS = "linux"

    fun resolveClasspathLibraries(libraries: List<LibraryEntry>): List<ResolvedLibrary> {
        return libraries
            .filter { isAllowedOnTargetOs(it) }
            .mapNotNull { entry ->
                val artifact = entry.downloads?.artifact ?: return@mapNotNull null
                ResolvedLibrary(
                    mavenName = entry.name,
                    relativePath = artifact.path,
                    sha1 = artifact.sha1,
                    sizeBytes = artifact.size,
                    downloadUrl = artifact.url
                )
            }
    }

    private fun isAllowedOnTargetOs(entry: LibraryEntry): Boolean {
        val rules = entry.rules
        // No rules at all means "always allowed" - this is the common case
        // for most libraries (only natives/platform-specific ones carry rules).
        if (rules.isNullOrEmpty()) return true

        // Mojang's rule evaluation is sequential: start disallowed, and each
        // matching rule sets the running verdict to its action. The last
        // matching rule wins - this mirrors the real launcher spec rather
        // than a simplified "any allow rule matches" approximation, which
        // would produce wrong results for entries that both allow globally
        // and disallow a specific OS.
        var allowed = false
        for (rule in rules) {
            val osConstraint = rule.os
            val matches = osConstraint == null || osConstraint.name == null || osConstraint.name == TARGET_OS
            if (matches) {
                allowed = rule.action == "allow"
            }
        }
        return allowed
    }

    fun resolveClasspathFiles(librariesDir: File, resolved: List<ResolvedLibrary>): List<File> =
        resolved.map { File(librariesDir, it.relativePath) }
}
