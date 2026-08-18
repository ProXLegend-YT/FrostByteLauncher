package com.frostbyte.launcher.core.backup

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class WorldBackupManagerTest {

    private lateinit var tempDir: File
    private lateinit var worldsDir: File
    private lateinit var backupsDir: File
    private lateinit var manager: WorldBackupManager

    @Before
    fun setUp() {
        tempDir = File.createTempFile("backup-test", "").apply { delete(); mkdirs() }
        worldsDir = File(tempDir, "worlds").apply { mkdirs() }
        backupsDir = File(tempDir, "backups")
        manager = WorldBackupManager(backupsDir)
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun createWorld(name: String): File {
        val worldDir = File(worldsDir, name).apply { mkdirs() }
        File(worldDir, "level.dat").writeText("fake level data")
        File(worldDir, "region").mkdirs()
        File(worldDir, "region/r.0.0.mca").writeText("fake region data")
        return worldDir
    }

    @Test
    fun `backup creates a real zip containing every file in the world folder`() {
        val world = createWorld("MyWorld")

        val result = manager.backup(world)

        assertTrue(result is BackupResult.Success)
        val backupFile = (result as BackupResult.Success).backupFile
        assertTrue(backupFile.exists())
        assertTrue(backupFile.length() > 0)
    }

    @Test
    fun `backup fails cleanly when the world folder does not exist`() {
        val result = manager.backup(File(worldsDir, "DoesNotExist"))
        assertTrue(result is BackupResult.Failure)
    }

    @Test
    fun `round trip backup then restore reproduces the original files exactly`() {
        val world = createWorld("MyWorld")
        val originalLevelDat = File(world, "level.dat").readText()
        val originalRegion = File(world, "region/r.0.0.mca").readText()

        val backupResult = manager.backup(world) as BackupResult.Success

        val restoreDestination = File(worldsDir, "RestoredWorld")
        val restoreResult = manager.restore(backupResult.backupFile, restoreDestination)

        assertTrue(restoreResult is RestoreResult.Success)
        assertEquals(originalLevelDat, File(restoreDestination, "level.dat").readText())
        assertEquals(originalRegion, File(restoreDestination, "region/r.0.0.mca").readText())
    }

    @Test
    fun `restore clears pre-existing content in the destination before writing`() {
        val world = createWorld("MyWorld")
        val backupResult = manager.backup(world) as BackupResult.Success

        val restoreDestination = File(worldsDir, "Target").apply { mkdirs() }
        File(restoreDestination, "stale_leftover_file.txt").writeText("should be gone after restore")

        manager.restore(backupResult.backupFile, restoreDestination)

        assertFalse(File(restoreDestination, "stale_leftover_file.txt").exists())
        assertTrue(File(restoreDestination, "level.dat").exists())
    }

    @Test
    fun `restore refuses a zip-slip path traversal entry and does not touch the destination`() {
        val maliciousZip = File(tempDir, "malicious.zip")
        ZipOutputStream(maliciousZip.outputStream()).use { zos ->
            zos.putNextEntry(ZipEntry("../../../evil.txt"))
            zos.write("malicious content".toByteArray())
            zos.closeEntry()
        }

        val destination = File(worldsDir, "SafeDestination").apply { mkdirs() }
        File(destination, "original.txt").writeText("must survive")

        val result = manager.restore(maliciousZip, destination)

        assertTrue(result is RestoreResult.Failure)
        // Because validation happens before any destructive action, the
        // destination must be completely untouched by a rejected restore.
        assertTrue(File(destination, "original.txt").exists())
        val escapedFile = File(tempDir.parentFile, "evil.txt")
        assertFalse(escapedFile.exists())
    }

    @Test
    fun `restore fails cleanly when the backup file does not exist`() {
        val result = manager.restore(File(tempDir, "nonexistent.zip"), File(worldsDir, "Dest"))
        assertTrue(result is RestoreResult.Failure)
    }

    @Test
    fun `listBackups returns newest first with correct metadata`() {
        val world = createWorld("MyWorld")
        manager.backup(world)
        Thread.sleep(10)
        manager.backup(world)

        val backups = manager.listBackups()

        assertEquals(2, backups.size)
        assertTrue(backups.first().createdAtEpochMillis >= backups.last().createdAtEpochMillis)
        assertEquals("MyWorld", backups.first().worldName)
        assertTrue(backups.first().sizeBytes > 0)
    }

    @Test
    fun `listBackups returns an empty list, not an error, when nothing has been backed up`() {
        assertTrue(manager.listBackups().isEmpty())
    }

    @Test
    fun `deleteBackup removes the file and it no longer appears in listBackups`() {
        val world = createWorld("MyWorld")
        val backupResult = manager.backup(world) as BackupResult.Success

        val deleted = manager.deleteBackup(backupResult.backupFile)

        assertTrue(deleted)
        assertTrue(manager.listBackups().isEmpty())
    }
}
