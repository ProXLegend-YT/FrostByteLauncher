package com.frostbyte.launcher.core.runtime

import java.io.File
import java.util.concurrent.TimeUnit

/**
 * ============================================================================
 * READ THIS BEFORE MODIFYING - this is the single most technically
 * constrained subsystem in FrostByte, and it's easy to quietly reintroduce a
 * wrong assumption here. Three hard facts, verified rather than assumed:
 *
 * 1. Mojang's own published JRE builds (from piston-meta's java-runtime
 *    manifest) are glibc-linked x86_64/aarch64 Linux binaries. Android's
 *    kernel is Linux, but its userspace C library is Bionic, not glibc.
 *    A glibc binary will NOT run on stock Android - there is no compatible
 *    dynamic linker for it. FrostByte therefore does NOT download or exec
 *    Mojang's own "linux" JRE entries as a runnable JVM.
 *
 * 2. What DOES work: a JRE specifically built against Bionic/Android, of
 *    which the most established source is Termux's JDK packages (built for
 *    exactly this purpose - running a JVM inside a normal, non-rooted
 *    Android app sandbox). This is the same class of solution real Android
 *    Minecraft launchers use.
 *
 * 3. Getting a JRE to run at all is necessary but NOT sufficient to run
 *    Minecraft - LWJGL (Minecraft's window/OpenGL/audio bindings) also needs
 *    an Android-compatible native build and a windowing/GL translation
 *    layer, since Android has no X11/GLFW desktop surface. That problem is
 *    NOT solved by this class and is tracked as its own, larger item - see
 *    docs/KNOWN_GAPS.md. JavaRuntimeManager's job ends at "we have a working
 *    java binary we can execute and query `java -version` from."
 * ============================================================================
 *
 * Manages downloading, verifying, and locating a real, Android-executable
 * JRE for a given required major Java version (8, 17, 21, etc., per
 * Section 8 of the PRD - Java Runtime Manager).
 */
class JavaRuntimeManager(
    private val runtimesDir: File
) {
    /**
     * Returns the installed runtime's `java` executable for the given major
     * version, or null if it isn't installed yet - callers should then
     * trigger a download via the Download Manager using a resolved runtime
     * build descriptor.
     */
    fun installedJavaBinary(majorVersion: Int): File? {
        val dir = runtimeDirFor(majorVersion)
        val binary = File(dir, "bin/java")
        return if (binary.exists() && binary.canExecute()) binary else null
    }

    fun runtimeDirFor(majorVersion: Int): File =
        File(runtimesDir, "jre-$majorVersion").apply { mkdirs() }

    /**
     * Marks the java binary executable after extraction. Android requires
     * this explicitly - files extracted from a downloaded archive don't
     * carry a usable executable bit by default, and W^X restrictions on
     * modern Android mean this must happen in a location the app is allowed
     * to execute from (app-private storage, which runtimesDir already is -
     * see FileManager.javaRuntimesDir()).
     */
    fun markExecutable(majorVersion: Int): Boolean {
        val binary = File(runtimeDirFor(majorVersion), "bin/java")
        return binary.exists() && binary.setExecutable(true, true)
    }

    /**
     * Verifies an installed runtime actually runs, by invoking `java
     * -version` and checking the process exits cleanly. This is the real
     * verification step - a JRE archive extracting without error does NOT
     * guarantee the binary is compatible with this device's ABI.
     */
    fun verifyRuntimeRuns(majorVersion: Int): RuntimeVerification {
        val binary = installedJavaBinary(majorVersion)
            ?: return RuntimeVerification.NotInstalled

        return try {
            val process = ProcessBuilder(binary.absolutePath, "-version")
                .redirectErrorStream(true)
                .start()
            val exited = process.waitFor(10, TimeUnit.SECONDS)
            if (!exited) {
                process.destroyForcibly()
                return RuntimeVerification.TimedOut
            }
            if (process.exitValue() == 0) {
                RuntimeVerification.Working
            } else {
                RuntimeVerification.ExitedWithError(process.exitValue())
            }
        } catch (e: Exception) {
            // Most commonly: Exec format error (ABI mismatch) or
            // Permission denied (W^X blocking execution from this path).
            RuntimeVerification.FailedToStart(e.message ?: e::class.simpleName ?: "Unknown error")
        }
    }
}

sealed class RuntimeVerification {
    data object Working : RuntimeVerification()
    data object NotInstalled : RuntimeVerification()
    data object TimedOut : RuntimeVerification()
    data class ExitedWithError(val exitCode: Int) : RuntimeVerification()
    data class FailedToStart(val reason: String) : RuntimeVerification()
}
