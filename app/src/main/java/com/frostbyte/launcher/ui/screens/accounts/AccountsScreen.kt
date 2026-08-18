package com.frostbyte.launcher.ui.screens.accounts

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.frostbyte.launcher.core.auth.MinecraftSession
import com.frostbyte.launcher.core.auth.SignInState
import com.frostbyte.launcher.ui.common.frostByteViewModel
import com.frostbyte.launcher.ui.components.GlassCard
import com.frostbyte.launcher.ui.space.SpaceRenderer
import com.frostbyte.launcher.ui.theme.IceBlue
import com.frostbyte.launcher.ui.theme.StatusGreen
import com.frostbyte.launcher.ui.theme.StatusRed
import com.frostbyte.launcher.ui.theme.TextSecondary

/**
 * Accounts screen, Section 5 of the PRD. Microsoft account sign-in ONLY -
 * there is no offline/local/third-party account option here or anywhere
 * else in the codebase, by design.
 */
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel = frostByteViewModel { container -> AccountsViewModel(container.authRepository) }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        SpaceRenderer(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Accounts",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                val session = uiState.currentSession
                if (session != null) {
                    SignedInCard(session = session, onSignOut = viewModel::signOut)
                } else {
                    SignedOutCard(signInState = uiState.signInState, onSignInClick = viewModel::startSignIn)
                }
            }
        }
    }
}

@Composable
private fun SignedInCard(session: MinecraftSession, onSignOut: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = IceBlue, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(session.minecraftUsername, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("Microsoft account", style = MaterialTheme.typography.bodyMedium, color = StatusGreen)
                }
            }
            OutlinedButton(onClick = onSignOut) { Text("Sign out") }
        }
    }
}

@Composable
private fun SignedOutCard(signInState: SignInState, onSignInClick: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column {
            Text(
                text = "No account signed in",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "FrostByte only supports signing in with a genuine Microsoft account.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (signInState) {
                is SignInState.Idle -> {
                    Button(
                        onClick = onSignInClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = IceBlue, contentColor = Color(0xFF04040C))
                    ) {
                        Text("Sign in with Microsoft", fontWeight = FontWeight.Bold)
                    }
                }
                is SignInState.AwaitingUserAction -> {
                    DeviceCodeInstructions(signInState)
                }
                is SignInState.ExchangingXboxLive,
                is SignInState.ExchangingXsts,
                is SignInState.LoggingIntoMinecraft,
                is SignInState.VerifyingOwnership -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = IceBlue, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(stageLabel(signInState), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                is SignInState.Success -> {
                    // Briefly shown before AccountsViewModel promotes this
                    // into currentSession and this whole branch stops
                    // rendering in favor of SignedInCard.
                    Text("Signed in!", color = StatusGreen, style = MaterialTheme.typography.bodyMedium)
                }
                is SignInState.Failed -> {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(Icons.Filled.Error, contentDescription = null, tint = StatusRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(signInState.reason, style = MaterialTheme.typography.bodyMedium, color = StatusRed)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onSignInClick,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = IceBlue, contentColor = Color(0xFF04040C))
                    ) {
                        Text("Try again")
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceCodeInstructions(state: SignInState.AwaitingUserAction) {
    Column {
        Text(
            text = "1. On any device, go to:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = state.verificationUri,
            style = MaterialTheme.typography.titleMedium,
            color = IceBlue,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "2. Enter this code:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = state.userCode,
            style = MaterialTheme.typography.displayLarge,
            color = IceBlue,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TextSecondary, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Waiting for you to finish signing in…", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

private fun stageLabel(state: SignInState): String = when (state) {
    is SignInState.ExchangingXboxLive -> "Connecting to Xbox Live…"
    is SignInState.ExchangingXsts -> "Authorizing…"
    is SignInState.LoggingIntoMinecraft -> "Signing into Minecraft…"
    is SignInState.VerifyingOwnership -> "Verifying your Minecraft ownership…"
    else -> ""
}
