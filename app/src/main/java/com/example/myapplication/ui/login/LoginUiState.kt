package com.example.myapplication.ui.login

import com.example.myapplication.data.auth.SocialProvider

/** How the user is trying to sign in. [analyticsName] is the analytics `method` value. */
enum class AuthMethod(val analyticsName: String) {
    PASSWORD("password"),
    PASSKEY("passkey"),
    GOOGLE("google"),
    APPLE("apple"),
}

fun SocialProvider.toAuthMethod(): AuthMethod = when (this) {
    SocialProvider.GOOGLE -> AuthMethod.GOOGLE
    SocialProvider.APPLE -> AuthMethod.APPLE
}

enum class EmailError { INVALID_FORMAT }

/** Errors shown inline above the Log in button. */
sealed interface FormError {
    data object InvalidCredentials : FormError
    data class LockedOut(val retryAfterMinutes: Int) : FormError
}

/** Transient, dismissible messages shown in a snackbar. */
sealed interface MessageKind {
    data object NoPasskey : MessageKind
    data class Cancelled(val method: AuthMethod) : MessageKind
    data class SignInFailed(val method: AuthMethod) : MessageKind
    data object NetworkError : MessageKind
    data object ServerError : MessageKind
}

data class UserMessage(
    val id: Long,
    val kind: MessageKind,
    val canRetry: Boolean,
)

data class PendingLink(val email: String, val provider: SocialProvider) {
    override fun toString(): String = "PendingLink(email=<redacted>, provider=$provider)"
}

/**
 * Everything the login screen renders.
 *
 * Note: [password] lives only in memory here; it is never written to SavedStateHandle,
 * logged, or included in [toString].
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val rememberMe: Boolean = false,
    val emailError: EmailError? = null,
    val formError: FormError? = null,
    val loadingMethod: AuthMethod? = null,
    val message: UserMessage? = null,
    val pendingLink: PendingLink? = null,
    val isLoggedIn: Boolean = false,
) {
    val isLoading: Boolean get() = loadingMethod != null

    val isEmailValid: Boolean get() = EmailValidator.isValid(email)

    /** Email valid and password non-empty, regardless of loading. */
    val isFormValid: Boolean get() = isEmailValid && password.isNotEmpty()

    /** Whether tapping Log in does anything right now. */
    val isLoginEnabled: Boolean get() = isFormValid && !isLoading

    override fun toString(): String =
        "LoginUiState(email=<redacted>, password=<redacted>, isPasswordVisible=$isPasswordVisible, " +
            "rememberMe=$rememberMe, emailError=$emailError, formError=$formError, " +
            "loadingMethod=$loadingMethod, message=$message, pendingLink=$pendingLink, isLoggedIn=$isLoggedIn)"
}

object EmailValidator {
    // Pragmatic check (local@domain.tld). android.util.Patterns isn't usable in plain JVM unit tests.
    private val EMAIL_REGEX = Regex(
        "^[A-Za-z0-9.!#\$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?" +
            "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)*\\.[A-Za-z]{2,}$"
    )

    fun isValid(email: String): Boolean = EMAIL_REGEX.matches(email.trim())
}
