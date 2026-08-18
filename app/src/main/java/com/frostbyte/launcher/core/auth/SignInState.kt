package com.frostbyte.launcher.core.auth

sealed class SignInState {
    data object Idle : SignInState()
    data class AwaitingUserAction(val verificationUri: String, val userCode: String, val expiresAtEpochMillis: Long) : SignInState()
    data object ExchangingXboxLive : SignInState()
    data object ExchangingXsts : SignInState()
    data object LoggingIntoMinecraft : SignInState()
    data object VerifyingOwnership : SignInState()
    data class Success(val session: MinecraftSession) : SignInState()
    data class Failed(val reason: String) : SignInState()
}
