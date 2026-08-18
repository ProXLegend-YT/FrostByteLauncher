package com.frostbyte.launcher.core.network.model

/**
 * Models for the real Microsoft -> Xbox Live -> XSTS -> Minecraft Services
 * OAuth chain, per Section 5 of the PRD (Microsoft account authentication
 * ONLY - no offline/cracked/third-party account support exists anywhere in
 * this file or any class that uses it).
 */

// Step 1: Microsoft OAuth2 device code flow (device_code grant - the right
// choice for a mobile app with no embedded browser redirect capability).
data class MsDeviceCodeResponse(
    val device_code: String,
    val user_code: String,
    val verification_uri: String,
    val expires_in: Int,
    val interval: Int
)

data class MsTokenResponse(
    val access_token: String,
    val refresh_token: String,
    val expires_in: Int,
    val token_type: String
)

data class MsTokenErrorResponse(
    val error: String, // "authorization_pending" | "expired_token" | "access_denied" | etc.
    val error_description: String? = null
)

// Step 2: Xbox Live authentication
data class XboxLiveAuthRequest(
    val Properties: XboxLiveAuthProperties,
    val RelyingParty: String = "http://auth.xboxlive.com",
    val TokenType: String = "JWT"
)

data class XboxLiveAuthProperties(
    val AuthMethod: String = "RPS",
    val SiteName: String = "user.auth.xboxlive.com",
    val RpsTicket: String // must be prefixed "d=" + the MS access token
)

data class XboxLiveAuthResponse(
    val Token: String,
    val DisplayClaims: XboxLiveDisplayClaims
)

data class XboxLiveDisplayClaims(
    val xui: List<XboxLiveUserInfo>
)

data class XboxLiveUserInfo(
    val uhs: String // user hash, needed for the XSTS/Minecraft step
)

// Step 3: XSTS authorization (same response shape as Xbox Live auth)
data class XstsAuthRequest(
    val Properties: XstsAuthProperties,
    val RelyingParty: String = "rp://api.minecraftservices.com/",
    val TokenType: String = "JWT"
)

data class XstsAuthProperties(
    val SandboxId: String = "RETAIL",
    val UserTokens: List<String>
)

// Step 4: Minecraft Services login
data class MinecraftLoginRequest(
    val identityToken: String // "XBL3.0 x=<uhs>;<xsts token>"
)

data class MinecraftLoginResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int
)

// Step 5: Minecraft profile (ownership + identity)
data class MinecraftProfileResponse(
    val id: String, // UUID, no dashes
    val name: String
)

/** Returned by Mojang with 404 when the signed-in account does not own Minecraft - a real, honest ownership check. */
data class MinecraftProfileErrorResponse(
    val path: String? = null,
    val errorType: String? = null,
    val error: String? = null,
    val errorMessage: String? = null
)
