#!/data/data/com.termux/files/usr/bin/bash
set -e

echo "Syncing repo..."
git pull --rebase origin main

echo "Writing ProfilesViewModel.kt..."
cat > app/src/main/java/com/frostbyte/launcher/ui/screens/profiles/ProfilesViewModel.kt << 'FILE_EOF'
package com.frostbyte.launcher.ui.screens.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostbyte.launcher.core.auth.AuthRepository
import com.frostbyte.launcher.core.common.FrostByteResult
import com.frostbyte.launcher.core.common.Loader
import com.frostbyte.launcher.core.storage.repository.Profile
import com.frostbyte.launcher.core.storage.repository.ProfileDraft
import com.frostbyte.launcher.core.storage.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfilesUiState(
    val profiles: List<Profile> = emptyList(),
    val isCreateDialogOpen: Boolean = false,
    val errorMessage: String? = null,
    val selectedProfileId: Long? = null,
    val launchBlockedReason: String? = null
) {
    val selectedProfile: Profile?
        get() = profiles.firstOrNull { it.id == selectedProfileId } ?: profiles.firstOrNull()
}

/** UI-only state not derived from the database (dialog visibility, transient errors). */
private data class TransientUiState(
    val isCreateDialogOpen: Boolean = false,
    val errorMessage: String? = null,
    val selectedProfileId: Long? = null,
    val launchBlockedReason: String? = null
)

class ProfilesViewModel(
    private val repository: ProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val transientState = MutableStateFlow(TransientUiState())

    val uiState: StateFlow<ProfilesUiState> = combine(
        repository.observeProfiles(),
        transientState
    ) { profiles, transient ->
        ProfilesUiState(
            profiles = profiles,
            isCreateDialogOpen = transient.isCreateDialogOpen,
            errorMessage = transient.errorMessage,
            selectedProfileId = transient.selectedProfileId,
            launchBlockedReason = transient.launchBlockedReason
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfilesUiState()
    )

    fun selectProfile(profileId: Long) {
        transientState.update { it.copy(selectedProfileId = profileId, launchBlockedReason = null) }
    }

    fun openCreateDialog() {
        transientState.update { it.copy(isCreateDialogOpen = true, errorMessage = null) }
    }

    fun dismissCreateDialog() {
        transientState.update { it.copy(isCreateDialogOpen = false) }
    }

    fun createProfile(name: String, minecraftVersion: String, loader: Loader, ramGb: Int) {
        viewModelScope.launch {
            val result = repository.createProfile(
                ProfileDraft(
                    name = name,
                    minecraftVersion = minecraftVersion,
                    loader = loader,
                    ramAllocationMb = ramGb * 1024,
                    gameDirectory = "profiles/${name.lowercase().replace(" ", "_")}"
                )
            )
            when (result) {
                is FrostByteResult.Success ->
                    transientState.update { it.copy(isCreateDialogOpen = false, errorMessage = null) }
                is FrostByteResult.Failure ->
                    transientState.update { it.copy(errorMessage = result.message) }
            }
        }
    }

    fun deleteProfile(profile: Profile) {
        viewModelScope.launch { repository.deleteProfile(profile) }
    }

    fun setAsDefault(profile: Profile) {
        viewModelScope.launch { repository.setAsDefault(profile.id) }
    }

    fun dismissError() {
        transientState.update { it.copy(errorMessage = null) }
    }

    /**
     * Same honest-blocker pattern as HomeViewModel.onPlayClicked() - reports
     * the real reason launching isn't possible yet rather than simulating
     * success. Kept in sync with Home's logic deliberately rather than
     * inventing a different story for this screen.
     */
    fun onPlayClicked(profile: Profile) {
        viewModelScope.launch {
            val session = authRepository.currentSession()
            val reason = if (session == null) {
                "No Microsoft account signed in. Go to Accounts to sign in."
            } else {
                "Signed in as ${session.minecraftUsername}, but launching isn't wired up yet."
            }
            transientState.update { it.copy(launchBlockedReason = reason) }
        }
    }

    fun dismissLaunchBlocked() {
        transientState.update { it.copy(launchBlockedReason = null) }
    }
}
FILE_EOF

echo "Writing ProfilesScreen.kt..."
cat > app/src/main/java/com/frostbyte/launcher/ui/screens/profiles/ProfilesScreen.kt << 'FILE_EOF'
package com.frostbyte.launcher.ui.screens.profiles

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
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
import com.frostbyte.launcher.ui.theme.StatusAmber
import com.frostbyte.launcher.ui.theme.StatusRed
import com.frostbyte.launcher.ui.theme.TextSecondary

@Composable
fun ProfilesScreen(
    viewModel: ProfilesViewModel = frostByteViewModel { container ->
        ProfilesViewModel(container.profileRepository, container.authRepository)
    }
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
        ) { innerPadding ->
            if (uiState.profiles.isEmpty()) {
                EmptyProfilesState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 20.dp,
                        bottom = innerPadding.calculateBottomPadding() + 20.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Column {
                            Text(
                                text = "Installations",
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Manage your Minecraft Java Edition installations",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                    items(uiState.profiles, key = { it.id }) { profile ->
                        ProfileCard(
                            profile = profile,
                            isSelected = uiState.selectedProfile?.id == profile.id,
                            onRowClick = { viewModel.selectProfile(profile.id) },
                            onPlayClick = { viewModel.onPlayClicked(profile) },
                            onSetDefault = { viewModel.setAsDefault(profile) },
                            onDelete = { viewModel.deleteProfile(profile) }
                        )
                    }

                    val selected = uiState.selectedProfile
                    if (selected != null) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                            ProfileDetailPanel(
                                profile = selected,
                                launchBlockedReason = uiState.launchBlockedReason,
                                onPlayClick = { viewModel.onPlayClicked(selected) }
                            )
                        }
                    }
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
private fun ProfileCard(
    profile: Profile,
    isSelected: Boolean,
    onRowClick: () -> Unit,
    onPlayClick: () -> Unit,
    onSetDefault: () -> Unit,
    onDelete: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(
                if (isSelected) {
                    Modifier.border(width = 1.5.dp, color = IceBlue, shape = shape)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onRowClick)
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = profile.name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                        if (profile.isDefault) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Default", style = MaterialTheme.typography.labelSmall, color = IceBlue)
                        }
                    }
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
                IconButton(onClick = onPlayClick) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play ${profile.name}", tint = IceBlue)
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
}

/**
 * Detail panel matching the reference design's "Installation Info" card.
 * The Play button here reports the same honest blocker HomeViewModel does -
 * no fabricated launch success - see ProfilesViewModel.onPlayClicked().
 */
@Composable
private fun ProfileDetailPanel(profile: Profile, launchBlockedReason: String?, onPlayClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "Installation Info",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            InfoRow(label = "Name", value = profile.name)
            InfoRow(label = "Version", value = profile.minecraftVersion)
            InfoRow(label = "Loader", value = profile.loader.displayName)
            InfoRow(label = "Java", value = "Java ${profile.javaRuntimeVersion}")
            InfoRow(label = "RAM", value = "${formatRamGb(profile.ramAllocationGb)} GB")
            InfoRow(label = "Location", value = profile.gameDirectory)
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onPlayClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = IceBlue,
                    contentColor = Color(0xFF04040C)
                )
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Play")
            }

            if (launchBlockedReason != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Info, contentDescription = null, tint = StatusAmber, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = launchBlockedReason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = StatusAmber
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
FILE_EOF

echo "Writing ProfilesViewModelTest.kt..."
cat > app/src/test/java/com/frostbyte/launcher/ui/screens/profiles/ProfilesViewModelTest.kt << 'FILE_EOF'
package com.frostbyte.launcher.ui.screens.profiles

import com.frostbyte.launcher.core.auth.AuthRepository
import com.frostbyte.launcher.core.auth.FakeSessionStore
import com.frostbyte.launcher.core.auth.MicrosoftAuthConfig
import com.frostbyte.launcher.core.common.Loader
import com.frostbyte.launcher.core.network.service.FakeMicrosoftAuthService
import com.frostbyte.launcher.core.network.service.FakeMinecraftAuthService
import com.frostbyte.launcher.core.network.service.FakeXboxAuthService
import com.frostbyte.launcher.core.storage.repository.FakeProfileDao
import com.frostbyte.launcher.core.storage.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ProfilesViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: ProfilesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val authRepository = AuthRepository(
            microsoftAuthService = FakeMicrosoftAuthService(),
            xboxAuthService = FakeXboxAuthService(),
            minecraftAuthService = FakeMinecraftAuthService(),
            sessionStore = FakeSessionStore(),
            config = MicrosoftAuthConfig(clientId = "test-client-id"),
            ioDispatcher = testDispatcher
        )
        viewModel = ProfilesViewModel(ProfileRepository(FakeProfileDao()), authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `dialog starts closed`() = runTest(testDispatcher) {
        assertFalse(viewModel.uiState.first().isCreateDialogOpen)
    }

    @Test
    fun `openCreateDialog and dismissCreateDialog toggle dialog state`() = runTest(testDispatcher) {
        // uiState.value stays at the stateIn() placeholder until the combine()
        // upstream emits at least once, so wait for the real emission rather
        // than reading .value synchronously right after a transientState update.
        viewModel.openCreateDialog()
        assertTrue(viewModel.uiState.first { it.isCreateDialogOpen }.isCreateDialogOpen)

        viewModel.dismissCreateDialog()
        assertFalse(viewModel.uiState.first { !it.isCreateDialogOpen }.isCreateDialogOpen)
    }

    @Test
    fun `createProfile with valid input adds profile and closes dialog`() = runTest(testDispatcher) {
        viewModel.openCreateDialog()
        viewModel.createProfile("Survival", "1.21.1", Loader.FABRIC, ramGb = 4)
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.profiles.isNotEmpty() }
        assertFalse(state.isCreateDialogOpen)
        assertEquals(1, state.profiles.size)
        assertEquals("Survival", state.profiles.first().name)
        assertEquals(4096, state.profiles.first().ramAllocationMb)
    }

    @Test
    fun `createProfile with blank name surfaces error and keeps dialog open`() = runTest(testDispatcher) {
        viewModel.openCreateDialog()
        viewModel.createProfile("   ", "1.21.1", Loader.FABRIC, ramGb = 4)
        advanceUntilIdle()

        val state = viewModel.uiState.first { it.errorMessage != null }
        assertTrue(state.isCreateDialogOpen)
        assertNotNull(state.errorMessage)
        assertEquals(0, state.profiles.size)
    }

    @Test
    fun `dismissError clears the error message`() = runTest(testDispatcher) {
        viewModel.createProfile("", "1.21.1", Loader.FABRIC, ramGb = 4)
        advanceUntilIdle()
        assertNotNull(viewModel.uiState.first { it.errorMessage != null }.errorMessage)

        viewModel.dismissError()
        assertEquals(null, viewModel.uiState.first { it.errorMessage == null }.errorMessage)
    }
}
FILE_EOF

echo "Committing and pushing..."
git add -A
git commit -m "Redesign Profiles screen to match Installations reference: tappable cards, detail panel, honest Play button wired to real auth state"
git push

echo "Done!"
