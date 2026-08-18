package com.frostbyte.launcher.core.launch

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.InputStreamReader

sealed class LaunchStage {
    data object Checking : LaunchStage()
    data object Preparing : LaunchStage()
    data object StartingJava : LaunchStage()
    data class Running(val process: Process) : LaunchStage()
    data class Output(val line: String) : LaunchStage()
    data class Exited(val exitCode: Int) : LaunchStage()
    data class Failed(val reason: String) : LaunchStage()
}

/**
 * Spawns and monitors the real Minecraft Java process. This is the class
 * that replaces HomeViewModel's timed UI simulation - see Section 11 of the
 * PRD ("do not fake successful launches"). It genuinely runs
 * ProcessBuilder against a real java binary and streams real stdout/stderr;
 * it does not report success unless the process actually started.
 *
 * IMPORTANT: as documented extensively in JavaRuntimeManager.kt, actually
 * reaching a running Minecraft window on a real device additionally
 * requires an Android-compatible JRE (not yet in JavaRuntimeCatalog - see
 * docs/KNOWN_GAPS.md) and an LWJGL/rendering compatibility layer (not yet
 * built at all). Until both of those exist, real invocations of this class
 * will correctly and visibly fail at process start (e.g. "Exec format
 * error") rather than silently pretending to succeed - that failure is
 * accurate information, not a bug in this class.
 */
class LauncherEngine(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    /**
     * Launches Minecraft and emits real lifecycle stages as they happen.
     * Collectors see actual stdout/stderr lines from the JVM process and a
     * real exit code - nothing here is simulated or pre-scripted.
     */
    fun launch(config: LaunchConfig): Flow<LaunchStage> = flow {
        emit(LaunchStage.Checking)

        if (!config.javaBinary.exists()) {
            emit(LaunchStage.Failed("Java runtime not found at ${config.javaBinary.absolutePath}"))
            return@flow
        }
        if (!config.javaBinary.canExecute()) {
            emit(LaunchStage.Failed("Java runtime is not executable (missing exec permission or blocked by system policy)"))
            return@flow
        }
        if (config.classpathEntries.any { !it.exists() }) {
            val missing = config.classpathEntries.filterNot { it.exists() }
            emit(LaunchStage.Failed("Missing ${missing.size} required classpath file(s), e.g. ${missing.first().name}"))
            return@flow
        }

        emit(LaunchStage.Preparing)
        config.gameDirectory.mkdirs()

        emit(LaunchStage.StartingJava)
        val command = ArgumentBuilder.buildCommand(config)

        val process = try {
            ProcessBuilder(command)
                .directory(config.gameDirectory)
                .redirectErrorStream(true)
                .start()
        } catch (e: Exception) {
            // Genuinely expected failure mode until JavaRuntimeCatalog has a
            // real, verified Android-compatible JRE build - see class doc.
            emit(LaunchStage.Failed("Failed to start Java process: ${e.message ?: e::class.simpleName}"))
            return@flow
        }

        emit(LaunchStage.Running(process))

        // Stream real process output as it happens, rather than buffering
        // it all and reporting at the end - matters for a long-lived
        // process where the user wants to see it's actually alive.
        // emit() here runs on ioDispatcher via the flowOn() at the bottom of
        // this function, which is the correct way to move a flow builder's
        // execution to another dispatcher - wrapping individual emit() calls
        // in withContext() is not allowed (Flow enforces that emissions
        // happen on the collector's context) and would throw at runtime.
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                emit(LaunchStage.Output(line!!))
            }
        }

        val exitCode = process.waitFor()
        emit(LaunchStage.Exited(exitCode))
    }.flowOn(ioDispatcher)

    fun kill(process: Process) {
        if (process.isAlive) {
            process.destroy()
        }
    }
}
