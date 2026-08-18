package com.frostbyte.launcher.core.backup

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

data class WorldBackupInfo(
    val file: File,
    val worldName: String,
    val createdAtEpochMillis: Long,
    val sizeBytes: Long
)

sealed class BackupResult {
    data class Success(val backupFile: File) : BackupResult()
    data class Failure(val reason: String) : BackupResult()
}

sealed class RestoreResult {
    data object Success : RestoreResult()
    data class Failure(val reason: String) : RestoreResult()
}

/**
 * Real, working zip-based world backup/restore - Section 9's "world
 * backups" requirement. No dependency on the JRE/LWJGL gap (see
 * docs/KNOWN_GAPS.md) since this only manipulates files on disk; a world
 * save folder is just files whether or not a game process can currently
 * run.
 */
class WorldBackupManager(private val backupsDir: File) {

    fun backup(worldDir: File): BackupResult {
        if (!worldDir.exists() || !worldDir.isDirectory) {
            return BackupResult.Failure("World folder not found: ${worldDir.absolutePath}")
        }
        backupsDir.mkdirs()
        val timestamp = System.currentTimeMillis()
        val backupFile = File(backupsDir, "${worldDir.name}_$timestamp.zip")

        return try {
            ZipOutputStream(backupFile.outputStream()).use { zos ->
                worldDir.walkTopDown().filter { it.isFile }.forEach { file ->
                    val relativePath = file.relativeTo(worldDir).path
                    zos.putNextEntry(ZipEntry(relativePath))
                    file.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
            BackupResult.Success(backupFile)
        } catch (e: Exception) {
            backupFile.delete() // don't leave a partial/corrupt zip behind on failure
            BackupResult.Failure("Backup failed: ${e.message ?: e::class.simpleName}")
        }
    }

    /**
     * Restores a backup zip into destinationWorldDir. Uses the same
     * zip-slip guard as NativesResolver.extract - refuses to write any
     * entry whose resolved path escapes destinationWorldDir, regardless of
     * what the zip's entry names claim. destinationWorldDir is cleared
     * first ONLY if the restore can proceed safely (all entries validated
     * before any destructive action).
     */
    fun restore(backupFile: File, destinationWorldDir: File): RestoreResult {
        if (!backupFile.exists()) {
            return RestoreResult.Failure("Backup file not found: ${backupFile.absolutePath}")
        }

        return try {
            ZipFile(backupFile).use { zip ->
                val entries = zip.entries().toList()

                // Validate every entry BEFORE touching the destination -
                // never partially clear a world folder only to fail
                // halfway through restoring it.
                for (entry in entries) {
                    val resolved = File(destinationWorldDir, entry.name).canonicalFile
                    if (!resolved.path.startsWith(destinationWorldDir.canonicalFile.path)) {
                        return RestoreResult.Failure("Backup contains an unsafe entry path: ${entry.name}")
                    }
                }

                destinationWorldDir.deleteRecursively()
                destinationWorldDir.mkdirs()

                for (entry in entries) {
                    val outFile = File(destinationWorldDir, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            outFile.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                }
            }
            RestoreResult.Success
        } catch (e: Exception) {
            RestoreResult.Failure("Restore failed: ${e.message ?: e::class.simpleName}")
        }
    }

    fun listBackups(): List<WorldBackupInfo> {
        if (!backupsDir.exists()) return emptyList()
        return backupsDir.listFiles { f -> f.isFile && f.extension == "zip" }
            ?.map {
                WorldBackupInfo(
                    file = it,
                    worldName = it.nameWithoutExtension.substringBeforeLast('_'),
                    createdAtEpochMillis = it.lastModified(),
                    sizeBytes = it.length()
                )
            }
            ?.sortedByDescending { it.createdAtEpochMillis }
            ?: emptyList()
    }

    fun deleteBackup(file: File): Boolean = file.delete()
}
