package com.frostbyte.launcher.ui.navigation

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.frostbyte.launcher.ui.theme.IceBlue
import com.frostbyte.launcher.ui.theme.SpaceVoid
import com.frostbyte.launcher.ui.theme.TextSecondary

/** Width threshold above which we switch from bottom nav to a side nav rail. */
private const val COMPACT_WIDTH_DP = 600

@Composable
fun FrostByteNavScaffold(
    navController: NavHostController,
    windowWidthDp: Int,
    content: @Composable (Modifier) -> Unit
) {
    val isCompact = windowWidthDp < COMPACT_WIDTH_DP
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Transparent bars are required so the cosmic background shows through
    // behind the nav chrome (Section 3: transparent glass UI over space).
    val navColors = NavigationBarItemDefaults.colors(
        selectedIconColor = SpaceVoid,
        selectedTextColor = IceBlue,
        indicatorColor = IceBlue,
        unselectedIconColor = TextSecondary,
        unselectedTextColor = TextSecondary
    )
    val railColors = NavigationRailItemDefaults.colors(
        selectedIconColor = SpaceVoid,
        selectedTextColor = IceBlue,
        indicatorColor = IceBlue,
        unselectedIconColor = TextSecondary,
        unselectedTextColor = TextSecondary
    )

    fun navigate(destination: FrostByteDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun isSelected(destination: FrostByteDestination): Boolean =
        currentDestination?.hierarchy?.any { it.route == destination.route } == true

    if (isCompact) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
                    FrostByteDestination.primaryDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = isSelected(destination),
                            onClick = { navigate(destination) },
                            icon = { Icon(destination.icon, contentDescription = destination.label) },
                            label = { Text(destination.label) },
                            colors = navColors
                        )
                    }
                }
            }
        ) {
            // Scaffold's innerPadding is intentionally not applied here - each
            // screen's own content (e.g. LazyColumn contentPadding) handles
            // insets itself, since the space background must extend full-bleed
            // behind the transparent nav bar rather than being padded away from it.
            content(Modifier.fillMaxSize())
        }
    } else {
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(containerColor = Color.Transparent) {
                FrostByteDestination.primaryDestinations.forEach { destination ->
                    NavigationRailItem(
                        selected = isSelected(destination),
                        onClick = { navigate(destination) },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                        colors = railColors
                    )
                }
            }
            content(Modifier.fillMaxSize())
        }
    }
}
