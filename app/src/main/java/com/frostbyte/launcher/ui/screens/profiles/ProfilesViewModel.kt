package com.frostbyte.launcher.ui.screens.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val errorMessage: String? = null
)

/** UI-only state not derived from the database (dialog visibility, transient errors). */
private data class TransientUiState(
    val isCreateDialogOpen: Boolean = false,
    val errorMessage: String? = null
)

class ProfilesViewModel(private val repository: ProfileRepository) : ViewModel() {

    private val transientState = MutableStateFlow(TransientUiState())

    val uiState: StateFlow<ProfilesUiState> = combine(
        repository.observeProfiles(),
        transientState
    ) { profiles, transient ->
        ProfilesUiState(
            profiles = profiles,
            isCreateDialogOpen = transient.isCreateDialogOpen,
            errorMessage = transient.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProfilesUiState()
    )

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
}
