package com.frostbyte.launcher.core.controls

import java.io.File

/**
 * Writes key bindings into options.txt using Minecraft's real, documented
 * line format: `key_key.forward:key.keyboard.w`. This is genuinely useful
 * today even without a live game process (see docs/KNOWN_GAPS.md on the
 * JRE/LWJGL gap) - once LauncherEngine can hand off to a real Minecraft
 * process, that process will read exactly this file, in exactly this
 * format, with no translation step needed.
 *
 * Only rewrites the key_* lines this app manages - any other existing
 * options.txt content (video settings, sound levels, etc. that a real game
 * session writes) is preserved untouched, since a real options.txt has many
 * more lines than just key bindings.
 */
object OptionsTxtWriter {

    fun write(optionsFile: File, bindings: Map<MinecraftAction, String>) {
        val existingLines = if (optionsFile.exists()) optionsFile.readLines() else emptyList()
        val managedKeys = MinecraftAction.entries.map { it.optionsKeyName }.toSet()

        // Keep every existing line EXCEPT the key_* lines this app manages -
        // preserves unrelated settings a real game session may have written.
        val preservedLines = existingLines.filterNot { line ->
            val key = line.substringBefore(':', missingDelimiterValue = "")
            key in managedKeys
        }

        val newKeyLines = MinecraftAction.entries.map { action ->
            val value = bindings[action] ?: action.defaultKey
            "${action.optionsKeyName}:$value"
        }

        optionsFile.parentFile?.mkdirs()
        optionsFile.writeText((preservedLines + newKeyLines).joinToString("\n"))
    }

    /** Reads back only the key_* bindings this app manages from an existing options.txt, ignoring every other line. */
    fun readBindings(optionsFile: File): Map<MinecraftAction, String> {
        if (!optionsFile.exists()) return emptyMap()
        val byOptionsKeyName = MinecraftAction.entries.associateBy { it.optionsKeyName }

        return optionsFile.readLines().mapNotNull { line ->
            val separatorIndex = line.indexOf(':')
            if (separatorIndex == -1) return@mapNotNull null
            val key = line.substring(0, separatorIndex)
            val value = line.substring(separatorIndex + 1)
            val action = byOptionsKeyName[key] ?: return@mapNotNull null
            action to value
        }.toMap()
    }
}
