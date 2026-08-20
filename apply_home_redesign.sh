#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "Syncing repo..."
git pull --rebase origin main

echo "Writing HomeScreen.kt..."
cat > app/src/main/java/com/frostbyte/launcher/ui/screens/home/HomeScreen.kt << 'FILE_EOF'
package com.frostbyte.launcher.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.frostbyte.launcher.core.storage.repository.Profile
import com.frostbyte.launcher.ui.common.formatRamGb
import com.frostbyte.launcher.ui.common.frostByteViewModel
import com.frostbyte.launcher.ui.components.GlassCard
import com.frostbyte.launcher.ui.space.SpaceRenderer
import com.frostbyte.launcher.ui.theme.IceBlue
import com.frostbyte.launcher.ui.theme.StatusAmber
import com.frostbyte.launcher.ui.theme.TextSecondary

/**
 * Home screen per Section 6 of the PRD. Shows branding, active profile
 * summary, and the primary PLAY action.
 *
 * Profile data is real (Room-backed via ProfileRepository, Phase 2). The
 * PLAY button reports a real, specific blocker (no Microsoft account signed
 * in yet) rather than simulating a launch - see HomeViewModel.
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = frostByteViewModel { container ->
        HomeViewModel(container.profileRepository, container.authRepository)
    }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        SpaceRenderer(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { HomeHeader() }

            val active = uiState.activeProfile
            if (active == null) {
                item { NoProfilesCard() }
            } else {
                item {
                    ActiveProfileCard(
                        profile = active,
                        launchState = uiState.launchState,
                        onPlayClick = viewModel::onPlayClicked
                    )
                }
                item { QuickActionsRow() }
                if (uiState.recentProfiles.isNotEmpty()) {
                    item {
                        Text(
                            text = "OTHER PROFILES",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    items(uiState.recentProfiles, key = { it.id }) { profile ->
                        RecentProfileRow(profile = profile)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader() {
    Column {
        Text(
            text = "FROSTBYTE",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Minecraft Java Edition — Beyond the Stars.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
    }
}

@Composable
private fun NoProfilesCard() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "No profiles yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Head to the Profiles tab to create your first Minecraft profile.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ActiveProfileCard(profile: Profile, launchState: LaunchState, onPlayClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "Welcome back!",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Let's explore new worlds",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "${profile.minecraftVersion} · ${profile.loader.displayName}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))

            // RAM allocation, shown as a real bar against the profile's own
            // configured ceiling - not a hardcoded "8 GB" max, since that
            // number varies per profile and per device.
            RamAllocationBar(profile = profile)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Java ${profile.javaRuntimeVersion}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(20.dp))

            when (val state = launchState) {
                is LaunchState.Idle -> {
                    PlayButton(onClick = onPlayClick)
                }
                is LaunchState.NotReady -> {
                    Column {
                        PlayButton(onClick = onPlayClick)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = StatusAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = state.reason,
                                style = MaterialTheme.typography.bodyMedium,
                                color = StatusAmber
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Shows RAM allocation as an actual progress bar, e.g. "4 GB / 8 GB" with a
 * filled track - matching the reference design's RAM bar. The ceiling used
 * here is the device's own total memory (so the bar means something real:
 * how much of THIS device's RAM this profile is configured to use), not an
 * arbitrary fixed number.
 */
@Composable
private fun RamAllocationBar(profile: Profile) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val totalDeviceRamMb = remember(context) {
        val activityManager = context.getSystemService(android.app.ActivityManager::class.java)
        val info = android.app.ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(info)
        val totalMb = (info.totalMem / (1024 * 1024)).toInt()
        if (totalMb > 0) totalMb else profile.ramAllocationMb // fallback if the service is ever unavailable
    }
    val totalDeviceRamGb = totalDeviceRamMb / 1024f
    val fraction = (profile.ramAllocationMb.toFloat() / totalDeviceRamMb.toFloat()).coerceIn(0f, 1f)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "RAM Allocation", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(
                text = "${formatRamGb(profile.ramAllocationGb)} GB / ${formatRamGb(totalDeviceRamGb)} GB",
                style = MaterialTheme.typography.bodySmall,
                color = IceBlue
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(color = TextSecondary.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .background(color = IceBlue, shape = RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
private fun PlayButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = IceBlue,
            contentColor = androidx.compose.ui.graphics.Color(0xFF04040C)
        )
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "Play", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun QuickActionsRow() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(title = "Install Mods", subtitle = "CurseForge · Modrinth", modifier = Modifier.weight(1f))
            QuickActionCard(title = "Install Shaders", subtitle = "Complementary · BSL", modifier = Modifier.weight(1f))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(title = "Resource Packs", subtitle = "Faithful · Realistic", modifier = Modifier.weight(1f))
            QuickActionCard(title = "Servers", subtitle = "Browse & join", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuickActionCard(title: String, subtitle: String, modifier: Modifier = Modifier) {
    GlassCard(modifier = modifier, contentPadding = 12.dp) {
        Column {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun RecentProfileRow(profile: Profile) {
    GlassCard(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = profile.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "${profile.minecraftVersion} · ${profile.loader.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            Text(text = "${formatRamGb(profile.ramAllocationGb)} GB", style = MaterialTheme.typography.labelLarge, color = IceBlue)
        }
    }
}
FILE_EOF

echo "Committing and pushing..."
git add -A
git commit -m "Redesign Home screen to match reference: Welcome Back hero, real RAM bar, 4-card quick-access grid"
git push

echo "Done!"
