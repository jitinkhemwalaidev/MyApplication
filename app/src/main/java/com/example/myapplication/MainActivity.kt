package com.example.myapplication

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.login.LoginRoute
import com.example.myapplication.ui.login.openSignUpInCustomTab
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The SCRUM-1 design is dark-only, so keep light system bar icons on the dark background.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme(darkTheme = true, dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MyApplicationApp()
                }
            }
        }
    }
}

/**
 * Minimal top-level destination switch. The login screen is the start destination;
 * Home and Forgot password are placeholders because they're out of scope for SCRUM-1.
 */
@Composable
fun MyApplicationApp() {
    val context = LocalContext.current
    var destination by rememberSaveable { mutableStateOf(ROUTE_LOGIN) }
    var resetEmail by rememberSaveable { mutableStateOf("") }

    when (destination) {
        ROUTE_LOGIN -> LoginRoute(
            onLoginSuccess = { destination = ROUTE_HOME },
            onForgotPassword = { email ->
                // TODO(SCRUM-1): Navigate to the real password-reset flow (separate ticket).
                resetEmail = email
                destination = ROUTE_FORGOT_PASSWORD
            },
            onSignUp = { openSignUpInCustomTab(context) },
        )
        ROUTE_HOME -> PlaceholderScreen(
            text = stringResource(R.string.placeholder_home),
            onBack = { destination = ROUTE_LOGIN },
        )
        ROUTE_FORGOT_PASSWORD -> PlaceholderScreen(
            text = stringResource(R.string.placeholder_forgot_password, resetEmail),
            onBack = { destination = ROUTE_LOGIN },
        )
    }
}

@Composable
private fun PlaceholderScreen(text: String, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        TextButton(onClick = onBack) { Text(stringResource(R.string.placeholder_back)) }
    }
}

private const val ROUTE_LOGIN = "login"
private const val ROUTE_HOME = "home"
private const val ROUTE_FORGOT_PASSWORD = "forgot_password"
