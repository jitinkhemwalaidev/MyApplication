package com.example.myapplication.data.auth

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Deterministic stand-in for the real authentication backend.
 *
 * Password sign-in:
 *  - [VALID_PASSWORD] -> Success
 *  - [NETWORK_ERROR_PASSWORD] -> NetworkError
 *  - [SERVER_ERROR_PASSWORD] -> ServerError
 *  - anything else -> InvalidCredentials, and LockedOut after [AuthConfig.maxFailedAttempts]
 *    failures for the same email (for [AuthConfig.lockoutMinutes]).
 * Passkey -> NoPasskey. Google -> AccountLinkRequired([LINK_REQUIRED_EMAIL]). Apple -> Success.
 * Linking -> Success.
 */
class FakeAuthRepository(
    private val config: AuthConfig = AuthConfig.Default,
    private val latencyMillis: Long = DEFAULT_LATENCY_MILLIS,
    private val clock: () -> Long = System::currentTimeMillis,
) : AuthRepository {

    private val mutex = Mutex()
    private val failedAttempts = mutableMapOf<String, Int>()
    private val lockedUntil = mutableMapOf<String, Long>()

    override suspend fun signInWithPassword(
        email: String,
        password: String,
        rememberMe: Boolean,
    ): AuthResult {
        // TODO(SCRUM-1): Call the real sign-in API here (HTTPS POST, password in the body only,
        //  never in the URL or logs). Use rememberMe to request the long-lived session.
        delay(latencyMillis)
        val key = email.lowercase()
        return mutex.withLock {
            val now = clock()
            val lockEnd = lockedUntil[key]
            if (lockEnd != null && now < lockEnd) {
                return@withLock AuthResult.LockedOut(minutesUntil(lockEnd, now))
            }
            when (password) {
                VALID_PASSWORD -> {
                    failedAttempts.remove(key)
                    lockedUntil.remove(key)
                    AuthResult.Success(newSession(rememberMe))
                }
                NETWORK_ERROR_PASSWORD -> AuthResult.NetworkError
                SERVER_ERROR_PASSWORD -> AuthResult.ServerError
                else -> {
                    val attempts = (failedAttempts[key] ?: 0) + 1
                    if (attempts >= config.maxFailedAttempts) {
                        failedAttempts.remove(key)
                        lockedUntil[key] = now + TimeUnit.MINUTES.toMillis(config.lockoutMinutes.toLong())
                        AuthResult.LockedOut(config.lockoutMinutes)
                    } else {
                        failedAttempts[key] = attempts
                        AuthResult.InvalidCredentials
                    }
                }
            }
        }
    }

    override suspend fun signInWithPasskey(): AuthResult {
        // TODO(SCRUM-1): Use androidx.credentials CredentialManager.getCredential() with a
        //  GetPublicKeyCredentialOption built from the server's WebAuthn challenge, then send the
        //  assertion to the backend. Map NoCredentialException -> NoPasskey and
        //  GetCredentialCancellationException -> Cancelled.
        delay(latencyMillis)
        return AuthResult.NoPasskey
    }

    override suspend fun signInWithGoogle(): AuthResult {
        // TODO(SCRUM-1): Use Credential Manager with GetGoogleIdOption (Sign in with Google), send
        //  the ID token to the backend, and return AccountLinkRequired when the backend reports an
        //  existing password account with the same email.
        delay(latencyMillis)
        return AuthResult.AccountLinkRequired(LINK_REQUIRED_EMAIL, SocialProvider.GOOGLE)
    }

    override suspend fun signInWithApple(): AuthResult {
        // TODO(SCRUM-1): Run the Sign in with Apple OAuth web flow (authorization code + PKCE in a
        //  Custom Tab, redirect back to the app) and exchange the code with the backend.
        delay(latencyMillis)
        return AuthResult.Success(newSession(rememberMe = false))
    }

    override suspend fun linkSocialAccount(email: String, provider: SocialProvider): AuthResult {
        // TODO(SCRUM-1): Call the real account-linking API.
        delay(latencyMillis)
        return AuthResult.Success(newSession(rememberMe = false))
    }

    private fun newSession(rememberMe: Boolean) = AuthSession(
        accessToken = "fake-${UUID.randomUUID()}",
        expiresAtEpochMillis = clock() + config.sessionDurationMillis(rememberMe),
    )

    private fun minutesUntil(end: Long, now: Long): Int {
        val minute = TimeUnit.MINUTES.toMillis(1)
        return ((end - now + minute - 1) / minute).toInt().coerceAtLeast(1)
    }

    companion object {
        const val DEFAULT_LATENCY_MILLIS = 800L
        const val VALID_PASSWORD = "password123"
        const val NETWORK_ERROR_PASSWORD = "network"
        const val SERVER_ERROR_PASSWORD = "server"
        const val LINK_REQUIRED_EMAIL = "jane@example.com"
    }
}
