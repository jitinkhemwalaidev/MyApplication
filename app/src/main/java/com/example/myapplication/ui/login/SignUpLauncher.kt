package com.example.myapplication.ui.login

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.example.myapplication.data.auth.REGISTRATION_URL

/** Opens the registration page in a Chrome Custom Tab, falling back to any browser. */
fun openSignUpInCustomTab(context: Context, url: String = REGISTRATION_URL) {
    val uri = url.toUri()
    try {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(context, uri)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // No browser installed; nothing sensible to do.
        }
    }
}
