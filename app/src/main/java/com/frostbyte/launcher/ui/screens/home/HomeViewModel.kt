package com.frostbyte.launcher.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostbyte.launcher.core.auth.AuthRepository
import com.frostbyte.launcher.core.storage.repository.Profile
import com.frostbyte.launcher.core.storage.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Real, user-facing launch state. NotReady exists specifically to surface
 * real blockers (no account signed in, JRE not installed, required files
 * not downloaded) instead of pretending the game could launch - see
 * Section 11 of the PRD ("do not fake successful launches").
 */
sealed interface LaunchState {
    data object Idle : LaunchState
    data class NotReady(val reason: String) : LaunchState
}

data class HomeUiState(
    val allProfiles: List<Profile> = emptyList(),
    val activeProfile: Profile? = null,
    val launchState: LaunchState = LaunchState.Idle
) {
    /** Profiles other than the active one, most-recently-played first (already sorted by the repo query). */
    val recentProfiles: List<Profile> get() = allProfiles.filter { it.id != activeProfile?.id }
}

/**
 * Home screen ViewModel. Profile data is real (Phase 2).
 *
 * The PLAY button now checks the REAL signed-in state via AuthRepository
 * (Phase 5). If nobody is signed in, it reports that specific, honest
 * blocker and points at the Accounts screen. If someone IS signed in, the
 * next real blocker in the chain is that this class does not yet call
 * LaunchPreparer/LauncherEngine (Phase 4's pipeline) - that wiring is a
 * self-contained follow-up, not done here, and onPlayClicked() says so
 * explicitly rather than silently doing nothing once auth is no longer the
 * blocker. At every stage, the reported reason reflects what is actually
 * true right now - never a fabricated success.
 */
class HomeViewModel(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val launchState = MutableStateFlow<LaunchState>(LaunchState.Idle)

    val uiState: StateFlow<HomeUiState> = combine(
        profileRepository.observeProfiles(),
        launchState
    ) { profiles, launch ->
        // "Active" profile: the one flagged isDefault, falling back to the
        // most recently played (first in the already-sorted list) so Home
        // always has something to show once at least one profile exists.
        val active = profiles.firstOrNull { it.isDefault } ?: profiles.firstOrNull()
        HomeUiState(allProfiles = profiles, activeProfile = active, launchState = launch)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun onPlayClicked() {
        if (uiState.value.activeProfile == null) return

        viewModelScope.launch {
            val session = authRepository.currentSession()
            val reason = if (session == null) {
                "No Microsoft account signed in. Go to Accounts to sign in."
            } else {
                // Genuinely the next true blocker - LaunchPreparer/LauncherEngine
                // (Phase 4) are fully built and tested but not yet called from
                // here. This is NOT a placeholder message dressed up to look
                // finished; it accurately reflects what this class does today.
                "Signed in as ${session.minecraftUsername}, but launching isn't wired up yet."
            }
            launchState.update { LaunchState.NotReady(reason) }
        }
    }

    fun dismissNotReady() {
        launchState.update { LaunchState.Idle }
    }
}
