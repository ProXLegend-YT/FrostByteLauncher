package com.frostbyte.launcher.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * All main navigation destinations (Section 5 of PRD).
 * Home/Versions/Profiles/Mods/Shaders are shown in the primary bottom nav /
 * nav rail; the rest are reachable from a "More" section or Settings, to
 * avoid cramming 13 destinations into a single bottom bar.
 */
sealed class FrostByteDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val isPrimary: Boolean
) {
    data object Home : FrostByteDestination("home", "Home", Icons.Filled.Home, true)
    data object Versions : FrostByteDestination("versions", "Versions", Icons.Filled.Public, true)
    data object Profiles : FrostByteDestination("profiles", "Profiles", Icons.Filled.Person, true)
    data object Mods : FrostByteDestination("mods", "Mods", Icons.Filled.Extension, true)
    data object Shaders : FrostByteDestination("shaders", "Shaders", Icons.Filled.WbSunny, true)

    data object ResourcePacks : FrostByteDestination("resource_packs", "Resource Packs", Icons.Filled.Widgets, false)
    data object Worlds : FrostByteDestination("worlds", "Worlds", Icons.Filled.Inventory2, false)
    data object Controls : FrostByteDestination("controls", "Controls", Icons.Filled.Gamepad, false)
    data object Accounts : FrostByteDestination("accounts", "Accounts", Icons.Filled.AccountCircle, false)
    data object Downloads : FrostByteDestination("downloads", "Downloads", Icons.Filled.Download, false)
    data object Performance : FrostByteDestination("performance", "Performance", Icons.Filled.Speed, false)
    data object Storage : FrostByteDestination("storage", "Storage", Icons.Filled.Storage, false)
    data object Settings : FrostByteDestination("settings", "Settings", Icons.Filled.Settings, false)

    companion object {
        val primaryDestinations = listOf(Home, Versions, Profiles, Mods, Shaders)
        val secondaryDestinations = listOf(
            ResourcePacks, Worlds, Controls, Accounts, Downloads, Performance, Storage, Settings
        )
        val all = primaryDestinations + secondaryDestinations
    }
}
