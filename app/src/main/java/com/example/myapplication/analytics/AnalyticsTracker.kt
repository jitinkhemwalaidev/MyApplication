package com.example.myapplication.analytics

import android.util.Log

/** Sends product analytics events. Never pass personal data (emails, passwords, tokens). */
interface AnalyticsTracker {
    fun track(event: String, params: Map<String, String> = emptyMap())
}

object AnalyticsEvents {
    const val LOGIN_ATTEMPT = "login_attempt"
    const val LOGIN_SUCCESS = "login_success"
    const val LOGIN_FAILURE = "login_failure"
    const val FORGOT_PASSWORD_TAPPED = "forgot_password_tapped"
    const val SIGN_UP_TAPPED = "sign_up_tapped"

    const val PARAM_METHOD = "method"
    const val PARAM_REASON = "reason"
}

/**
 * No-op tracker that only writes events to Logcat.
 *
 * TODO(SCRUM-1): Replace with the real analytics SDK.
 */
class LogcatAnalyticsTracker : AnalyticsTracker {
    override fun track(event: String, params: Map<String, String>) {
        Log.d(TAG, "$event $params")
    }

    private companion object {
        const val TAG = "Analytics"
    }
}
