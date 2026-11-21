package com.example.myapplication.data.auth

import java.util.concurrent.TimeUnit

/**
 * Session and lockout settings for SCRUM-1.
 *
 * TODO(SCRUM-1): The values below are placeholders. Product still has to confirm the
 *  remember-me session length (ticket open question 1) and the lockout thresholds
 *  (ticket open question 4). The real backend is expected to enforce lockout; the client
 *  only uses these values for the fake repository and for display.
 */
data class AuthConfig(
    val rememberMeSessionDays: Int = 30,
    val standardSessionHours: Int = 12,
    val maxFailedAttempts: Int = 5,
    val lockoutMinutes: Int = 5,
) {
    fun sessionDurationMillis(rememberMe: Boolean): Long =
        if (rememberMe) {
            TimeUnit.DAYS.toMillis(rememberMeSessionDays.toLong())
        } else {
            TimeUnit.HOURS.toMillis(standardSessionHours.toLong())
        }

    companion object {
        val Default = AuthConfig()
    }
}

/**
 * Where "Sign up" sends the user (opened in a Chrome Custom Tab).
 *
 * TODO(SCRUM-1): Replace with the real registration URL.
 */
const val REGISTRATION_URL = "https://example.com/register"
