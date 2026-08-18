package com.frostbyte.launcher.core.filesystem

import android.content.Context
import java.io.File

/**
 * Narrow interface exposing only what ViewModels (VersionsViewModel,
 * ContentBrowserViewModel, and similar download-triggering ViewModels)
 * actually need from FileManager - resolving category directories. Lets
 * tests substitute a fake without needing a real Android Context, while
 * FileManager itself still exposes its full concrete API for callers that
 * need it (e.g. the Storage screen).
 */
interface GameDirectoryProvider {
    fun versionsDir(): File
    fun modsCacheDir(): File
    fun shadersCacheDir(): File
    fun resourcePacksCacheDir(): File
}

/**
 * Resolves and manages FrostByte's on-device directory layout, per Section 9
 * of the PRD (File Manager: organized storage for versions/mods/profiles,
 * disk usage reporting, safe deletion). All game-related files live under
 * app-private external storage (getExternalFilesDir), NOT shared/public
 * storage - this avoids needing broad storage permissions on modern Android
 * and keeps FrostByte's files sandboxed to itself.
 */
class FileManager(private val context: Context) : GameDirectoryProvider {

    private fun baseDir(): File =
        context.getExternalFilesDir(null) ?: context.filesDir

    override fun versionsDir(): File = File(baseDir(), "versions").apply { mkdirs() }
    fun librariesDir(): File = File(baseDir(), "libraries").apply { mkdirs() }
    fun assetsDir(): File = File(baseDir(), "assets").apply { mkdirs() }
    fun profilesDir(): File = File(baseDir(), "profiles").apply { mkdirs() }
    override fun modsCacheDir(): File = File(baseDir(), "mods_cache").apply { mkdirs() }
    override fun shadersCacheDir(): File = File(baseDir(), "shaders_cache").apply { mkdirs() }
    override fun resourcePacksCacheDir(): File = File(baseDir(), "resource_packs_cache").apply { mkdirs() }
    fun javaRuntimesDir(): File = File(baseDir(), "java_runtimes").apply { mkdirs() }
    fun logsDir(): File = File(baseDir(), "logs").apply { mkdirs() }

    fun profileDirectory(relativePath: String): File =
        File(profilesDir().parentFile, relativePath).apply { mkdirs() }

    /** Recursively sums bytes used by a directory. Runs on whatever dispatcher the caller uses - callers should invoke this off the main thread. */
    fun directorySizeBytes(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    data class StorageBreakdown(
        val versionsBytes: Long,
        val librariesBytes: Long,
        val assetsBytes: Long,
        val modsCacheBytes: Long,
        val shadersCacheBytes: Long,
        val resourcePacksCacheBytes: Long,
        val javaRuntimesBytes: Long,
        val logsBytes: Long
    ) {
        val totalBytes: Long
            get() = versionsBytes + librariesBytes + assetsBytes + modsCacheBytes +
                shadersCacheBytes + resourcePacksCacheBytes + javaRuntimesBytes + logsBytes
    }

    /** Computes disk usage per category, for the Storage screen (Phase 9's full UI; this is the underlying data). */
    fun computeStorageBreakdown(): StorageBreakdown = StorageBreakdown(
        versionsBytes = directorySizeBytes(versionsDir()),
        librariesBytes = directorySizeBytes(librariesDir()),
        assetsBytes = directorySizeBytes(assetsDir()),
        modsCacheBytes = directorySizeBytes(modsCacheDir()),
        shadersCacheBytes = directorySizeBytes(shadersCacheDir()),
        resourcePacksCacheBytes = directorySizeBytes(resourcePacksCacheDir()),
        javaRuntimesBytes = directorySizeBytes(javaRuntimesDir()),
        logsBytes = directorySizeBytes(logsDir())
    )

    /**
     * Deletes a file/directory that must be located inside FrostByte's own
     * base directory - refuses to delete anything outside it. This is a
     * safety rail against a caller accidentally passing a path that resolves
     * outside the sandbox (e.g. from bad user input in a future "custom game
     * directory" feature).
     */
    fun safeDelete(target: File): Boolean {
        val base = baseDir().canonicalFile
        val resolved = target.canonicalFile
        if (!resolved.path.startsWith(base.path)) {
            return false
        }
        return resolved.deleteRecursively()
    }

    fun clearCache() {
        modsCacheDir().deleteRecursively()
        shadersCacheDir().deleteRecursively()
        resourcePacksCacheDir().deleteRecursively()
        // Recreate the (now-deleted) directories immediately so subsequent
        // callers relying on <cache>Dir() returning an existing directory
        // don't hit surprises.
        modsCacheDir(); shadersCacheDir(); resourcePacksCacheDir()
    }
}
