package com.example.myapplication.ui.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.AppDependencies
import com.example.myapplication.analytics.AnalyticsEvents
import com.example.myapplication.analytics.AnalyticsTracker
import com.example.myapplication.data.auth.AuthRepository
import com.example.myapplication.data.auth.AuthResult
import com.example.myapplication.data.auth.SocialProvider
import com.example.myapplication.data.auth.TokenStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val tokenStorage: TokenStorage,
    private val analytics: AnalyticsTracker,
) : ViewModel() {

    // Only the email and the remember-me choice survive process death. The password never does.
    private val _uiState = MutableStateFlow(
        LoginUiState(
            email = savedStateHandle[KEY_EMAIL] ?: "",
            rememberMe = savedStateHandle[KEY_REMEMBER_ME] ?: false,
        )
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var emailFieldFocused = false
    private var lastAction: AuthAction? = null
    private var nextMessageId = 0L

    fun onEmailChange(value: String) {
        savedStateHandle[KEY_EMAIL] = value
        _uiState.update {
            it.copy(
                email = value,
                // Once an error is showing, clear it as soon as the input becomes valid.
                emailError = if (it.emailError != null && !EmailValidator.isValid(value)) it.emailError else null,
                formError = null,
            )
        }
    }

    /** Validates the email format when the field loses focus. */
    fun onEmailFocusChange(isFocused: Boolean) {
        if (isFocused) {
            emailFieldFocused = true
        } else if (emailFieldFocused) {
            emailFieldFocused = false
            validateEmail()
        }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, formError = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onRememberMeChange(checked: Boolean) {
        savedStateHandle[KEY_REMEMBER_ME] = checked
        _uiState.update { it.copy(rememberMe = checked) }
    }

    fun onLoginClick() {
        val state = _uiState.value
        if (state.isLoading) return
        if (!state.isFormValid) {
            validateEmail()
            return
        }
        perform(AuthAction.Password)
    }

    fun onPasskeyClick() = perform(AuthAction.Passkey)

    fun onSocialClick(provider: SocialProvider) = perform(AuthAction.Social(provider))

    fun onConfirmLink() {
        val link = _uiState.value.pendingLink ?: return
        perform(AuthAction.Link(link.email, link.provider))
    }

    /** Cancel on the link dialog: back to the login screen with no error. */
    fun onDismissLink() {
        _uiState.update { it.copy(pendingLink = null) }
    }

    fun onMessageShown(id: Long) {
        _uiState.update { if (it.message?.id == id) it.copy(message = null) else it }
    }

    /** Retries the last action, keeping whatever the user typed. */
    fun onRetry() {
        val action = lastAction ?: return
        if (action == AuthAction.Password) onLoginClick() else perform(action)
    }

    fun onForgotPasswordClick() {
        analytics.track(AnalyticsEvents.FORGOT_PASSWORD_TAPPED)
    }

    fun onSignUpClick() {
        analytics.track(AnalyticsEvents.SIGN_UP_TAPPED)
    }

    fun onLoginSuccessHandled() {
        _uiState.update { it.copy(isLoggedIn = false) }
    }

    private fun validateEmail() {
        _uiState.update {
            val invalid = it.email.isNotBlank() && !EmailValidator.isValid(it.email)
            it.copy(emailError = if (invalid) EmailError.INVALID_FORMAT else null)
        }
    }

    private fun perform(action: AuthAction) {
        val state = _uiState.value
        if (state.isLoading) return // blocks double taps and parallel sign-ins
        lastAction = action
        val method = action.method
        analytics.track(AnalyticsEvents.LOGIN_ATTEMPT, mapOf(AnalyticsEvents.PARAM_METHOD to method.analyticsName))
        _uiState.update {
            it.copy(loadingMethod = method, formError = null, message = null, pendingLink = null)
        }
        // Snapshot the inputs so later edits don't change an in-flight request.
        val email = state.email.trim()
        val password = state.password
        val rememberMe = state.rememberMe
        viewModelScope.launch {
            val result = when (action) {
                AuthAction.Password -> authRepository.signInWithPassword(email, password, rememberMe)
                AuthAction.Passkey -> authRepository.signInWithPasskey()
                is AuthAction.Social -> when (action.provider) {
                    SocialProvider.GOOGLE -> authRepository.signInWithGoogle()
                    SocialProvider.APPLE -> authRepository.signInWithApple()
                }
                is AuthAction.Link -> authRepository.linkSocialAccount(action.email, action.provider)
            }
            handleResult(method, result)
        }
    }

    private fun handleResult(method: AuthMethod, result: AuthResult) {
        when (result) {
            is AuthResult.Success -> {
                tokenStorage.saveSession(result.session)
                analytics.track(AnalyticsEvents.LOGIN_SUCCESS, mapOf(AnalyticsEvents.PARAM_METHOD to method.analyticsName))
                lastAction = null
                _uiState.update {
                    it.copy(loadingMethod = null, password = "", isPasswordVisible = false, isLoggedIn = true)
                }
            }
            is AuthResult.AccountLinkRequired -> {
                _uiState.update {
                    it.copy(loadingMethod = null, pendingLink = PendingLink(result.email, result.provider))
                }
            }
            AuthResult.InvalidCredentials -> {
                trackFailure(method, "invalid_credentials")
                if (method == AuthMethod.PASSWORD) {
                    _uiState.update { it.copy(loadingMethod = null, formError = FormError.InvalidCredentials) }
                } else {
                    showMessage(MessageKind.SignInFailed(method), canRetry = false)
                }
            }
            is AuthResult.LockedOut -> {
                trackFailure(method, "locked_out")
                _uiState.update {
                    it.copy(loadingMethod = null, formError = FormError.LockedOut(result.retryAfterMinutes))
                }
            }
            AuthResult.Cancelled -> {
                trackFailure(method, "cancelled")
                showMessage(MessageKind.Cancelled(method), canRetry = false)
            }
            AuthResult.NoPasskey -> {
                trackFailure(method, "no_passkey")
                showMessage(MessageKind.NoPasskey, canRetry = false)
            }
            AuthResult.NetworkError -> {
                trackFailure(method, "network_error")
                showMessage(MessageKind.NetworkError, canRetry = true)
            }
            AuthResult.ServerError -> {
                trackFailure(method, "server_error")
                showMessage(MessageKind.ServerError, canRetry = true)
            }
        }
    }

    private fun trackFailure(method: AuthMethod, reason: String) {
        analytics.track(
            AnalyticsEvents.LOGIN_FAILURE,
            mapOf(AnalyticsEvents.PARAM_METHOD to method.analyticsName, AnalyticsEvents.PARAM_REASON to reason),
        )
    }

    private fun showMessage(kind: MessageKind, canRetry: Boolean) {
        _uiState.update {
            it.copy(loadingMethod = null, message = UserMessage(nextMessageId++, kind, canRetry))
        }
    }

    private sealed interface AuthAction {
        val method: AuthMethod

        data object Password : AuthAction {
            override val method = AuthMethod.PASSWORD
        }

        data object Passkey : AuthAction {
            override val method = AuthMethod.PASSKEY
        }

        data class Social(val provider: SocialProvider) : AuthAction {
            override val method = provider.toAuthMethod()
        }

        data class Link(val email: String, val provider: SocialProvider) : AuthAction {
            override val method = provider.toAuthMethod()
        }
    }

    companion object {
        private const val KEY_EMAIL = "login_email"
        private const val KEY_REMEMBER_ME = "login_remember_me"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                LoginViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    authRepository = AppDependencies.authRepository,
                    tokenStorage = AppDependencies.tokenStorage,
                    analytics = AppDependencies.analyticsTracker,
                )
            }
        }
    }
}
