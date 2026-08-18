package com.frostbyte.launcher.ui.screens.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.frostbyte.launcher.core.common.Loader
import com.frostbyte.launcher.core.storage.repository.Profile
import com.frostbyte.launcher.ui.common.formatRamGb
import com.frostbyte.launcher.ui.common.frostByteViewModel
import com.frostbyte.launcher.ui.components.GlassCard
import com.frostbyte.launcher.ui.space.SpaceRenderer
import com.frostbyte.launcher.ui.theme.IceBlue
import com.frostbyte.launcher.ui.theme.StatusRed
import com.frostbyte.launcher.ui.theme.TextSecondary

@Composable
fun ProfilesScreen(
    viewModel: ProfilesViewModel = frostByteViewModel { container -> ProfilesViewModel(container.profileRepository) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        SpaceRenderer(modifier = Modifier.fillMaxSize())

        Scaffold(
            containerColor = Color.Transparent,
            floatingActionButton = {
                FloatingActionButton(
                    onClick = viewModel::openCreateDialog,
                    containerColor = IceBlue,
                    contentColor = Color(0xFF04040C)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Create profile")
                }
            }
        ) {
            if (uiState.profiles.isEmpty()) {
                EmptyProfilesState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Profiles",
                            style = MaterialTheme.typography.displayLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    items(uiState.profiles, key = { it.id }) { profile ->
                        ProfileCard(
                            profile = profile,
                            onSetDefault = { viewModel.setAsDefault(profile) },
                            onDelete = { viewModel.deleteProfile(profile) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) } // room for FAB
                }
            }
        }

        if (uiState.isCreateDialogOpen) {
            CreateProfileDialog(
                errorMessage = uiState.errorMessage,
                onDismiss = viewModel::dismissCreateDialog,
                onCreate = viewModel::createProfile
            )
        }
    }
}

@Composable
private fun EmptyProfilesState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        GlassCard {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "No profiles yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap + to create your first profile",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(profile: Profile, onSetDefault: () -> Unit, onDelete: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = profile.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${profile.minecraftVersion} · ${profile.loader.displayName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = IceBlue
                )
                Text(
                    text = "${formatRamGb(profile.ramAllocationGb)} GB RAM · Java ${profile.javaRuntimeVersion}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            IconButton(onClick = onSetDefault) {
                Icon(
                    imageVector = if (profile.isDefault) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = if (profile.isDefault) "Default profile" else "Set as default",
                    tint = if (profile.isDefault) IceBlue else TextSecondary
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete profile", tint = StatusRed)
            }
        }
    }
}

@Composable
private fun CreateProfileDialog(
    errorMessage: String?,
    onDismiss: () -> Unit,
    onCreate: (name: String, version: String, loader: Loader, ramGb: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var version by remember { mutableStateOf("1.21.1") }
    var loaderExpanded by remember { mutableStateOf(false) }
    var selectedLoader by remember { mutableStateOf(Loader.FABRIC) }
    var ramGb by remember { mutableFloatStateOf(4f) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Profile name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = version,
                    onValueChange = { version = it },
                    label = { Text("Minecraft version") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = loaderExpanded,
                    onExpandedChange = { loaderExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedLoader.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Loader") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = loaderExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    DropdownMenu(
                        expanded = loaderExpanded,
                        onDismissRequest = { loaderExpanded = false }
                    ) {
                        Loader.entries.forEach { loader ->
                            DropdownMenuItem(
                                text = { Text(loader.displayName) },
                                onClick = {
                                    selectedLoader = loader
                                    loaderExpanded = false
                                }
                            )
                        }
                    }
                }
                Column {
                    Text("RAM: ${ramGb.toInt()} GB", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = ramGb,
                        onValueChange = { ramGb = it },
                        valueRange = 1f..12f,
                        steps = 10
                    )
                }
                if (errorMessage != null) {
                    Text(text = errorMessage, color = StatusRed, style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name, version, selectedLoader, ramGb.toInt()) }) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
