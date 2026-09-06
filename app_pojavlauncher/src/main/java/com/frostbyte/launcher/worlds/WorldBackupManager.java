package com.frostbyte.launcher.worlds;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Zip-based world backup/restore, kept as a genuinely new capability rather than a UI reskin —
 * neither PojavLauncher nor MojoLauncher offer this. A world save folder is just files on disk,
 * so this works whether or not a game process is currently running.
 *
 * This is a plain-Java port of an existing, already-reviewed Kotlin implementation
 * (WorldBackupManager.kt) that lived in this repo's disconnected Compose module (:app), which
 * is not wired into the actual build (see settings.gradle — only :app_pojavlauncher and its
 * native submodules are included). Porting to Java rather than pulling in the Kotlin toolchain
 * for one feature avoids a real, non-trivial build-system change on top of everything else this
 * project already depends on. The restore-side zip-slip guard (validate every entry path before
 * any destructive delete) is preserved exactly.
 */
public class WorldBackupManager {

    public static class WorldBackupInfo {
        public final File file;
        public final String worldName;
        public final long createdAtEpochMillis;
        public final long sizeBytes;

        WorldBackupInfo(File file, String worldName, long createdAtEpochMillis, long sizeBytes) {
            this.file = file;
            this.worldName = worldName;
            this.createdAtEpochMillis = createdAtEpochMillis;
            this.sizeBytes = sizeBytes;
        }
    }

    public static abstract class BackupResult {
        public static class Success extends BackupResult {
            public final File backupFile;
            public Success(File backupFile) { this.backupFile = backupFile; }
        }
        public static class Failure extends BackupResult {
            public final String reason;
            public Failure(String reason) { this.reason = reason; }
        }
    }

    public static abstract class RestoreResult {
        public static class Success extends RestoreResult {}
        public static class Failure extends RestoreResult {
            public final String reason;
            public Failure(String reason) { this.reason = reason; }
        }
    }

    private final File backupsDir;

    public WorldBackupManager(File backupsDir) {
        this.backupsDir = backupsDir;
    }

    public BackupResult backup(File worldDir) {
        if (!worldDir.exists() || !worldDir.isDirectory()) {
            return new BackupResult.Failure("World folder not found: " + worldDir.getAbsolutePath());
        }
        //noinspection ResultOfMethodCallIgnored
        backupsDir.mkdirs();
        long timestamp = System.currentTimeMillis();
        File backupFile = new File(backupsDir, worldDir.getName() + "_" + timestamp + ".zip");

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(backupFile))) {
            zipDirectory(worldDir, worldDir, zos);
            return new BackupResult.Success(backupFile);
        } catch (IOException e) {
            //noinspection ResultOfMethodCallIgnored
            backupFile.delete(); // don't leave a partial/corrupt zip behind on failure
            return new BackupResult.Failure("Backup failed: " + e.getMessage());
        }
    }

    private void zipDirectory(File root, File current, ZipOutputStream zos) throws IOException {
        File[] children = current.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) {
                zipDirectory(root, child, zos);
            } else {
                String relativePath = root.toURI().relativize(child.toURI()).getPath();
                zos.putNextEntry(new ZipEntry(relativePath));
                try (InputStream in = new FileInputStream(child)) {
                    copyStream(in, zos);
                }
                zos.closeEntry();
            }
        }
    }

    /**
     * Restores backupFile into destinationWorldDir. Uses the same zip-slip guard as the
     * original Kotlin version — refuses to write any entry whose resolved path escapes
     * destinationWorldDir, regardless of what the zip's entry names claim.
     * destinationWorldDir is only cleared if the restore can proceed safely (every entry is
     * validated before any destructive action is taken).
     */
    public RestoreResult restore(File backupFile, File destinationWorldDir) {
        if (!backupFile.exists()) {
            return new RestoreResult.Failure("Backup file not found: " + backupFile.getAbsolutePath());
        }

        try (ZipFile zip = new ZipFile(backupFile)) {
            List<? extends ZipEntry> entries = Collections.list(zip.entries());

            String destCanonicalPath = destinationWorldDir.getCanonicalFile().getPath();
            for (ZipEntry entry : entries) {
                File resolved = new File(destinationWorldDir, entry.getName()).getCanonicalFile();
                if (!resolved.getPath().startsWith(destCanonicalPath)) {
                    return new RestoreResult.Failure("Backup contains an unsafe entry path: " + entry.getName());
                }
            }

            deleteRecursively(destinationWorldDir);
            //noinspection ResultOfMethodCallIgnored
            destinationWorldDir.mkdirs();

            for (ZipEntry entry : entries) {
                File outFile = new File(destinationWorldDir, entry.getName());
                if (entry.isDirectory()) {
                    //noinspection ResultOfMethodCallIgnored
                    outFile.mkdirs();
                } else {
                    File parent = outFile.getParentFile();
                    if (parent != null) //noinspection ResultOfMethodCallIgnored
                        parent.mkdirs();
                    try (InputStream input = zip.getInputStream(entry);
                         OutputStream output = new FileOutputStream(outFile)) {
                        copyStream(input, output);
                    }
                }
            }
            return new RestoreResult.Success();
        } catch (IOException e) {
            return new RestoreResult.Failure("Restore failed: " + e.getMessage());
        }
    }

    public List<WorldBackupInfo> listBackups() {
        List<WorldBackupInfo> result = new ArrayList<>();
        if (!backupsDir.exists()) return result;
        File[] files = backupsDir.listFiles((dir, name) -> name.endsWith(".zip"));
        if (files == null) return result;
        for (File f : files) {
            String nameNoExt = f.getName().substring(0, f.getName().length() - 4);
            int lastUnderscore = nameNoExt.lastIndexOf('_');
            String worldName = lastUnderscore >= 0 ? nameNoExt.substring(0, lastUnderscore) : nameNoExt;
            result.add(new WorldBackupInfo(f, worldName, f.lastModified(), f.length()));
        }
        result.sort(Comparator.comparingLong((WorldBackupInfo w) -> w.createdAtEpochMillis).reversed());
        return result;
    }

    public boolean deleteBackup(File file) {
        return file.delete();
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    private static void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) deleteRecursively(child);
            }
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }
}
