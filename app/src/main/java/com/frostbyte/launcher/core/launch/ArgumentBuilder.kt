package com.frostbyte.launcher.core.launch

import java.io.File

/**
 * Parameters needed to construct a Minecraft launch command. Deliberately a
 * plain data holder with no Android/DB dependencies, so ArgumentBuilder is
 * pure and independently unit-testable.
 */
data class LaunchConfig(
    val javaBinary: File,
    val ramAllocationMb: Int,
    val extraJvmArguments: List<String>,
    val mainClass: String,
    val classpathEntries: List<File>,
    val nativesDirectory: File,
    val gameDirectory: File,
    val assetsDirectory: File,
    val assetIndexId: String,
    val minecraftVersion: String,
    val username: String,
    val uuid: String,
    val accessToken: String,
    val userType: String = "msa", // "msa" per Section 5 - Microsoft accounts only, no offline/legacy type ever produced here
    val windowWidth: Int? = null,
    val windowHeight: Int? = null
)

/**
 * Builds the full `java <args>` command line for launching Minecraft. This
 * mirrors the real argument structure Mojang's own launcher uses (JVM args,
 * then main class, then game args), so it's straightforward to diff against
 * an official launch command when debugging a real device failure.
 */
object ArgumentBuilder {

    fun buildCommand(config: LaunchConfig): List<String> {
        val command = mutableListOf(config.javaBinary.absolutePath)
        command += buildJvmArguments(config)
        command += config.mainClass
        command += buildGameArguments(config)
        return command
    }

    private fun buildJvmArguments(config: LaunchConfig): List<String> {
        val args = mutableListOf<String>()
        args += "-Xmx${config.ramAllocationMb}M"
        // Xms matches Xmx (no dynamic heap growth) - avoids a resize pause
        // during gameplay on a mobile device where every frame matters.
        args += "-Xms${config.ramAllocationMb}M"
        args += "-Djava.library.path=${config.nativesDirectory.absolutePath}"
        args += "-Dminecraft.launcher.brand=FrostByte"
        args += "-Dminecraft.launcher.version=1.0"
        args += config.extraJvmArguments.filter { it.isNotBlank() }
        args += "-cp"
        args += config.classpathEntries.joinToString(File.pathSeparator) { it.absolutePath }
        return args
    }

    private fun buildGameArguments(config: LaunchConfig): List<String> {
        val args = mutableListOf<String>()
        args += listOf("--username", config.username)
        args += listOf("--version", config.minecraftVersion)
        args += listOf("--gameDir", config.gameDirectory.absolutePath)
        args += listOf("--assetsDir", config.assetsDirectory.absolutePath)
        args += listOf("--assetIndex", config.assetIndexId)
        args += listOf("--uuid", config.uuid)
        args += listOf("--accessToken", config.accessToken)
        args += listOf("--userType", config.userType)
        args += listOf("--versionType", "release")
        if (config.windowWidth != null && config.windowHeight != null) {
            args += listOf("--width", config.windowWidth.toString())
            args += listOf("--height", config.windowHeight.toString())
        }
        return args
    }
}
