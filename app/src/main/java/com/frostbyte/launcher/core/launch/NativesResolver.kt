package com.frostbyte.launcher.core.launch

import com.frostbyte.launcher.core.network.model.LibraryEntry
import java.io.File
import java.util.zip.ZipFile

data class NativesLibrary(
    val mavenName: String,
    val sha1: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    val cacheRelativePath: String
)

sealed class NativesExtractionResult {
    data class Success(val extractedFiles: List<File>) : NativesExtractionResult()
    data class Failure(val reason: String) : NativesExtractionResult()
}

/**
 * Resolves and extracts native library classifiers (Section 8 of the PRD -
 * natives are what java.library.path in ArgumentBuilder points at). Some
 * older-format library entries carry natives under a "natives-linux"
 * classifier inside `downloads.classifiers`, separate from the main
 * platform-independent artifact.
 *
 * Extraction genuinely unzips the classifier jar and writes real .so files
 * to disk - it does not simulate or stub this step.
 */
object NativesResolver {

    private const val CLASSIFIER_KEY = "natives-linux"

    fun resolveNativesLibraries(libraries: List<LibraryEntry>): List<NativesLibrary> {
        return libraries.mapNotNull { entry ->
            val classifierArtifact = entry.downloads?.classifiers?.get(CLASSIFIER_KEY) ?: return@mapNotNull null
            NativesLibrary(
                mavenName = entry.name,
                sha1 = classifierArtifact.sha1,
                sizeBytes = classifierArtifact.size,
                downloadUrl = classifierArtifact.url,
                cacheRelativePath = classifierArtifact.path
            )
        }
    }

    /**
     * Extracts every .so entry from a downloaded natives jar into
     * [destinationDir]. Skips META-INF and non-.so entries (matching real
     * launcher behavior - natives jars often bundle license/manifest files
     * that have no business being extracted alongside actual native libs).
     * Uses a zip-slip guard: refuses to write any entry whose resolved path
     * escapes destinationDir, regardless of what the jar's entry names claim.
     */
    fun extract(jarFile: File, destinationDir: File): NativesExtractionResult {
        if (!jarFile.exists()) {
            return NativesExtractionResult.Failure("Natives jar not found: ${jarFile.absolutePath}")
        }
        destinationDir.mkdirs()
        val extracted = mutableListOf<File>()

        return try {
            ZipFile(jarFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.isDirectory) continue
                    if (entry.name.startsWith("META-INF/")) continue
                    if (!entry.name.endsWith(".so")) continue

                    val outFile = File(destinationDir, File(entry.name).name) // flatten - natives jars have no meaningful directory structure to preserve
                    val resolvedPath = outFile.canonicalFile
                    if (!resolvedPath.path.startsWith(destinationDir.canonicalFile.path)) {
                        // Zip-slip guard: a malicious/corrupt jar entry name
                        // (e.g. "../../etc/whatever") must never be allowed
                        // to write outside destinationDir.
                        return NativesExtractionResult.Failure("Refusing unsafe zip entry path: ${entry.name}")
                    }

                    zip.getInputStream(entry).use { input ->
                        outFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    outFile.setExecutable(false) // .so files are loaded via dlopen, not executed directly - no exec bit needed
                    extracted += outFile
                }
            }
            NativesExtractionResult.Success(extracted)
        } catch (e: Exception) {
            NativesExtractionResult.Failure("Failed to extract natives: ${e.message ?: e::class.simpleName}")
        }
    }
}
