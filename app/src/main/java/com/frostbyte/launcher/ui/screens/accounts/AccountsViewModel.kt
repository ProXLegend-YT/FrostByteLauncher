package com.frostbyte.launcher.ui.screens.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.frostbyte.launcher.core.auth.AuthRepository
import com.frostbyte.launcher.core.auth.MinecraftSession
import com.frostbyte.launcher.core.auth.SignInState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountsUiState(
    val currentSession: MinecraftSession? = null,
    val signInState: SignInState = SignInState.Idle
)

class AccountsViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountsUiState(currentSession = authRepository.currentSession()))
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    fun startSignIn() {
        viewModelScope.launch {
            authRepository.signIn().collect { state ->
                _uiState.update { it.copy(signInState = state) }
                if (state is SignInState.Success) {
                    _uiState.update { it.copy(currentSession = state.session) }
                }
            }
        }
    }

    fun dismissSignInResult() {
        _uiState.update { it.copy(signInState = SignInState.Idle) }
    }

    fun signOut() {
        authRepository.signOut()
        _uiState.update { it.copy(currentSession = null, signInState = SignInState.Idle) }
    }
}
