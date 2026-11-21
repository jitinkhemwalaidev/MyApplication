package com.example.myapplication.ui.login

import com.example.myapplication.analytics.AnalyticsTracker
import com.example.myapplication.data.auth.AuthRepository
import com.example.myapplication.data.auth.AuthResult
import com.example.myapplication.data.auth.AuthSession
import com.example.myapplication.data.auth.SocialProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.Dispatchers
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

val testSession = AuthSession(accessToken = "token", expiresAtEpochMillis = 0L)

/** Scriptable repository that records calls. Set [gate] to hold a call in flight. */
class TestAuthRepository : AuthRepository {
    var passwordResult: AuthResult = AuthResult.Success(testSession)
    var passkeyResult: AuthResult = AuthResult.Success(testSession)
    var googleResult: AuthResult = AuthResult.Success(testSession)
    var appleResult: AuthResult = AuthResult.Success(testSession)
    var linkResult: AuthResult = AuthResult.Success(testSession)
    var gate: CompletableDeferred<Unit>? = null

    data class PasswordCall(val email: String, val password: String, val rememberMe: Boolean)

    val passwordCalls = mutableListOf<PasswordCall>()
    var passkeyCalls = 0
    var googleCalls = 0
    var appleCalls = 0
    val linkCalls = mutableListOf<Pair<String, SocialProvider>>()

    override suspend fun signInWithPassword(email: String, password: String, rememberMe: Boolean): AuthResult {
        passwordCalls += PasswordCall(email, password, rememberMe)
        gate?.await()
        return passwordResult
    }

    override suspend fun signInWithPasskey(): AuthResult {
        passkeyCalls++
        gate?.await()
        return passkeyResult
    }

    override suspend fun signInWithGoogle(): AuthResult {
        googleCalls++
        gate?.await()
        return googleResult
    }

    override suspend fun signInWithApple(): AuthResult {
        appleCalls++
        gate?.await()
        return appleResult
    }

    override suspend fun linkSocialAccount(email: String, provider: SocialProvider): AuthResult {
        linkCalls += email to provider
        gate?.await()
        return linkResult
    }
}

class RecordingAnalytics : AnalyticsTracker {
    val events = mutableListOf<Pair<String, Map<String, String>>>()
    override fun track(event: String, params: Map<String, String>) {
        events += event to params
    }
}
