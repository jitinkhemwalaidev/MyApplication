package com.example.myapplication.data.auth

/** Social identity providers supported on the login screen. */
enum class SocialProvider { GOOGLE, APPLE }

/**
 * A signed-in session. [toString] is redacted so the token can never end up in logs or crash reports.
 */
class AuthSession(
    val accessToken: String,
    val expiresAtEpochMillis: Long,
) {
    override fun toString(): String = "AuthSession(accessToken=<redacted>, expiresAtEpochMillis=$expiresAtEpochMillis)"
}

/** Every outcome an authentication call can have. */
sealed interface AuthResult {
    data class Success(val session: AuthSession) : AuthResult

    /** Wrong email or password. Deliberately doesn't say which one. */
    data object InvalidCredentials : AuthResult

    /** A social sign-in matched an existing email account; the user must confirm linking. */
    data class AccountLinkRequired(val email: String, val provider: SocialProvider) : AuthResult

    /** The user backed out of a system sheet (passkey, Google or Apple). */
    data object Cancelled : AuthResult

    /** No passkey is available for this app on the device. */
    data object NoPasskey : AuthResult

    /** Too many failed attempts. */
    data class LockedOut(val retryAfterMinutes: Int) : AuthResult

    data object NetworkError : AuthResult
    data object ServerError : AuthResult
}

/** Authentication backend used by the login screen. */
interface AuthRepository {
    /** [rememberMe] selects the long-lived session length, see [AuthConfig]. */
    suspend fun signInWithPassword(email: String, password: String, rememberMe: Boolean): AuthResult

    suspend fun signInWithPasskey(): AuthResult

    suspend fun signInWithGoogle(): AuthResult

    suspend fun signInWithApple(): AuthResult

    /** Called after the user confirms linking on [AuthResult.AccountLinkRequired]. */
    suspend fun linkSocialAccount(email: String, provider: SocialProvider): AuthResult
}
