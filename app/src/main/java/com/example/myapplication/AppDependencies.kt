package com.example.myapplication

import com.example.myapplication.analytics.AnalyticsTracker
import com.example.myapplication.analytics.LogcatAnalyticsTracker
import com.example.myapplication.data.auth.AuthRepository
import com.example.myapplication.data.auth.FakeAuthRepository
import com.example.myapplication.data.auth.InMemoryTokenStorage
import com.example.myapplication.data.auth.TokenStorage

/** Minimal app-wide service locator (the project has no DI framework yet). */
object AppDependencies {
    // TODO(SCRUM-1): Swap FakeAuthRepository for the real API-backed implementation.
    val authRepository: AuthRepository by lazy { FakeAuthRepository() }
    val tokenStorage: TokenStorage by lazy { InMemoryTokenStorage() }
    val analyticsTracker: AnalyticsTracker by lazy { LogcatAnalyticsTracker() }
}
