package com.frostbyte.launcher.core.auth

/**
 * Microsoft identity platform requires every application to register as an
 * Azure AD app and use its own client ID - this is not optional and cannot
 * be worked around; Mojang/Microsoft's auth endpoints reject requests with
 * an invalid or missing client ID.
 *
 * FrostByte does not have a registered Azure AD application yet. This is a
 * real release blocker (tracked in docs/KNOWN_GAPS.md), not a coding task -
 * whoever owns/distributes this app needs to register one at
 * https://portal.azure.com under "App registrations", enable the
 * "XboxLive.signin" and "offline_access" delegated permissions, and set the
 * redirect URI appropriately for the device code flow used here.
 *
 * clientId is deliberately left blank in the default instance rather than
 * filled with a placeholder-that-looks-real, so a missing registration
 * fails loudly and immediately (AuthRepository checks this and refuses to
 * start the flow) instead of silently sending a bogus request to
 * Microsoft's servers. Modeled as a data class (not a global object) so
 * AuthRepository takes it as a constructor parameter - this keeps
 * AuthRepository testable with a real, non-blank client ID in tests while
 * production code wires in the real (currently blank) MicrosoftAuthConfig.default.
 */
data class MicrosoftAuthConfig(val clientId: String) {

    fun isConfigured(): Boolean = clientId.isNotBlank()

    companion object {
        val default = MicrosoftAuthConfig(clientId = "")

        const val DEVICE_CODE_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode"
        const val TOKEN_URL = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token"
        const val SCOPE = "XboxLive.signin offline_access"

        const val XBOX_LIVE_AUTH_URL = "https://user.auth.xboxlive.com/user/authenticate"
        const val XSTS_AUTH_URL = "https://xsts.auth.xboxlive.com/xsts/authorize"
        const val MINECRAFT_LOGIN_URL = "https://api.minecraftservices.com/authentication/login_with_xbox"
        const val MINECRAFT_PROFILE_URL = "https://api.minecraftservices.com/minecraft/profile"
    }
}
