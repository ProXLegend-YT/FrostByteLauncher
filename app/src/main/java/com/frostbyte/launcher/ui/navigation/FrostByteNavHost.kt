package com.frostbyte.launcher.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.frostbyte.launcher.ui.screens.accounts.AccountsScreen
import com.frostbyte.launcher.ui.screens.controls.ControlsScreen
import com.frostbyte.launcher.ui.screens.downloads.DownloadsScreen
import com.frostbyte.launcher.ui.screens.home.HomeScreen
import com.frostbyte.launcher.ui.screens.mods.ModsScreen
import com.frostbyte.launcher.ui.screens.performance.PerformanceScreen
import com.frostbyte.launcher.ui.screens.profiles.ProfilesScreen
import com.frostbyte.launcher.ui.screens.resourcepacks.ResourcePacksScreen
import com.frostbyte.launcher.ui.screens.settings.SettingsScreen
import com.frostbyte.launcher.ui.screens.shaders.ShadersScreen
import com.frostbyte.launcher.ui.screens.storage.StorageScreen
import com.frostbyte.launcher.ui.screens.versions.VersionsScreen
import com.frostbyte.launcher.ui.screens.worlds.WorldsScreen

@Composable
fun FrostByteNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = FrostByteDestination.Home.route,
        modifier = modifier
    ) {
        composable(FrostByteDestination.Home.route) { HomeScreen() }
        composable(FrostByteDestination.Versions.route) { VersionsScreen() }
        composable(FrostByteDestination.Profiles.route) { ProfilesScreen() }
        composable(FrostByteDestination.Mods.route) { ModsScreen() }
        composable(FrostByteDestination.Shaders.route) { ShadersScreen() }
        composable(FrostByteDestination.ResourcePacks.route) { ResourcePacksScreen() }
        composable(FrostByteDestination.Worlds.route) { WorldsScreen() }
        composable(FrostByteDestination.Controls.route) { ControlsScreen() }
        composable(FrostByteDestination.Accounts.route) { AccountsScreen() }
        composable(FrostByteDestination.Downloads.route) { DownloadsScreen() }
        composable(FrostByteDestination.Performance.route) { PerformanceScreen() }
        composable(FrostByteDestination.Storage.route) { StorageScreen() }
        composable(FrostByteDestination.Settings.route) { SettingsScreen() }
    }
}
