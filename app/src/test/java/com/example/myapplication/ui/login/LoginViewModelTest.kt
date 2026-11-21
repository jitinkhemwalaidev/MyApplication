package com.example.myapplication.ui.login

import androidx.lifecycle.SavedStateHandle
import com.example.myapplication.analytics.AnalyticsEvents
import com.example.myapplication.data.auth.AuthResult
import com.example.myapplication.data.auth.InMemoryTokenStorage
import com.example.myapplication.data.auth.SocialProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = TestAuthRepository()
    private val tokenStorage = InMemoryTokenStorage()
    private val analytics = RecordingAnalytics()
    private val savedStateHandle = SavedStateHandle()

    private fun createViewModel() = LoginViewModel(savedStateHandle, repository, tokenStorage, analytics)

    private fun LoginViewModel.fillValidForm(password: String = "secret") {
        onEmailChange("user@example.com")
        onPasswordChange(password)
    }

    private val state get() = vm.uiState.value
    private lateinit var vm: LoginViewModel

    // region Validation and button state

    @Test
    fun `email error appears only after the field loses focus`() {
        vm = createViewModel()
        vm.onEmailFocusChange(true)
        vm.onEmailChange("not-an-email")
        assertNull(state.emailError)

        vm.onEmailFocusChange(false)
        assertEquals(EmailError.INVALID_FORMAT, state.emailError)
    }

    @Test
    fun `initial unfocused callback does not show an error`() {
        vm = createViewModel()
        vm.onEmailChange("bad")
        vm.onEmailFocusChange(false)
        assertNull(state.emailError)
    }

    @Test
    fun `blank email on blur shows no error`() {
        vm = createViewModel()
        vm.onEmailFocusChange(true)
        vm.onEmailFocusChange(false)
        assertNull(state.emailError)
    }

    @Test
    fun `email error clears once the email becomes valid`() {
        vm = createViewModel()
        vm.onEmailFocusChange(true)
        vm.onEmailChange("user@")
        vm.onEmailFocusChange(false)
        assertNotNull(state.emailError)

        vm.onEmailChange("user@example")
        assertNotNull(state.emailError)
        vm.onEmailChange("user@example.com")
        assertNull(state.emailError)
    }

    @Test
    fun `login is disabled until email is valid and password is not empty`() {
        vm = createViewModel()
        assertFalse(state.isLoginEnabled)

        vm.onEmailChange("user@example.com")
        assertFalse(state.isLoginEnabled)

        vm.onPasswordChange("x")
        assertTrue(state.isLoginEnabled)

        vm.onEmailChange("user@example")
        assertFalse(state.isLoginEnabled)
    }

    @Test
    fun `login click with invalid form does not call the repository`() = runTest {
        vm = createViewModel()
        vm.onEmailChange("bad")
        vm.onPasswordChange("x")
        vm.onLoginClick()
        advanceUntilIdle()

        assertTrue(repository.passwordCalls.isEmpty())
        assertEquals(EmailError.INVALID_FORMAT, state.emailError)
    }

    @Test
    fun `password is hidden by default and toggles`() {
        vm = createViewModel()
        assertFalse(state.isPasswordVisible)
        vm.onTogglePasswordVisibility()
        assertTrue(state.isPasswordVisible)
        vm.onTogglePasswordVisibility()
        assertFalse(state.isPasswordVisible)
    }

    // endregion

    // region Loading and submit

    @Test
    fun `loading blocks a second submit`() = runTest {
        repository.gate = CompletableDeferred()
        vm = createViewModel()
        vm.fillValidForm()

        vm.onLoginClick()
        advanceUntilIdle()
        assertEquals(AuthMethod.PASSWORD, state.loadingMethod)
        assertFalse(state.isLoginEnabled)

        vm.onLoginClick()
        vm.onPasskeyClick()
        vm.onSocialClick(SocialProvider.GOOGLE)
        advanceUntilIdle()
        assertEquals(1, repository.passwordCalls.size)
        assertEquals(0, repository.passkeyCalls)
        assertEquals(0, repository.googleCalls)

        repository.gate!!.complete(Unit)
        advanceUntilIdle()
        assertNull(state.loadingMethod)
    }

    @Test
    fun `remember me is passed through to the repository`() = runTest {
        vm = createViewModel()
        vm.fillValidForm()
        vm.onRememberMeChange(true)
        vm.onLoginClick()
        advanceUntilIdle()

        // Success clears the password, so type it again for the second attempt.
        vm.onPasswordChange("secret")
        vm.onRememberMeChange(false)
        vm.onLoginClick()
        advanceUntilIdle()

        assertEquals(listOf(true, false), repository.passwordCalls.map { it.rememberMe })
    }

    @Test
    fun `typing keeps the lockout message but clears the invalid-credentials message`() = runTest {
        repository.passwordResult = AuthResult.LockedOut(retryAfterMinutes = 5)
        vm = createViewModel()
        vm.fillValidForm()
        vm.onLoginClick()
        advanceUntilIdle()
        vm.onPasswordChange("other")
        assertEquals(FormError.LockedOut(5), state.formError)

        repository.passwordResult = AuthResult.InvalidCredentials
        vm.onEmailChange("someone@example.com")
        assertEquals(FormError.LockedOut(5), state.formError)
        vm.onLoginClick()
        advanceUntilIdle()
        assertEquals(FormError.InvalidCredentials, state.formError)
        vm.onPasswordChange("again")
        assertNull(state.formError)
    }

    @Test
    fun `state toString does not leak the email via the pending link`() = runTest {
        repository.googleResult = AuthResult.AccountLinkRequired("jane@example.com", SocialProvider.GOOGLE)
        vm = createViewModel()
        vm.onSocialClick(SocialProvider.GOOGLE)
        advanceUntilIdle()
        assertFalse(state.toString().contains("jane@example.com"))
    }

    @Test
    fun `remember me is passed to passkey, social and link sign-ins`() = runTest {
        repository.googleResult = AuthResult.AccountLinkRequired("jane@example.com", SocialProvider.GOOGLE)
        repository.passkeyResult = AuthResult.NoPasskey
        vm = createViewModel()
        vm.onRememberMeChange(true)
        vm.onPasskeyClick()
        advanceUntilIdle()
        vm.onSocialClick(SocialProvider.GOOGLE)
        advanceUntilIdle()
        vm.onConfirmLink()
        advanceUntilIdle()

        assertEquals(listOf(true, true, true), repository.socialRememberMe)
    }

    @Test
    fun `email is trimmed before sign in`() = runTest {
        vm = createViewModel()
        vm.onEmailChange("  user@example.com ")
        vm.onPasswordChange("pw")
        vm.onLoginClick()
        advanceUntilIdle()
        assertEquals("user@example.com", repository.passwordCalls.single().email)
    }

    @Test
    fun `success stores the session, clears the password and signals navigation`() = runTest {
        vm = createViewModel()
        vm.fillValidForm()
        vm.onLoginClick()
        advanceUntilIdle()

        assertTrue(state.isLoggedIn)
        assertEquals("", state.password)
        assertSame(testSession, tokenStorage.getSession())
        assertTrue(
            analytics.events.contains(
                AnalyticsEvents.LOGIN_SUCCESS to mapOf(AnalyticsEvents.PARAM_METHOD to "password")
            )
        )

        vm.onLoginSuccessHandled()
        assertFalse(state.isLoggedIn)
    }

    // endregion

    // region Error mapping

    @Test
    fun `invalid credentials shows the generic inline error and keeps input`() = runTest {
        repository.passwordResult = AuthResult.InvalidCredentials
        vm = createViewModel()
        vm.fillValidForm("wrong")
        vm.onLoginClick()
        advanceUntilIdle()

        assertEquals(FormError.InvalidCredentials, state.formError)
        assertEquals("user@example.com", state.email)
        assertEquals("wrong", state.password)
        assertNull(state.message)

        vm.onPasswordChange("wrong2")
        assertNull(state.formError)
    }

    @Test
    fun `locked out shows the lockout error`() = runTest {
        repository.passwordResult = AuthResult.LockedOut(5)
        vm = createViewModel()
        vm.fillValidForm()
        vm.onLoginClick()
        advanceUntilIdle()
        assertEquals(FormError.LockedOut(5), state.formError)
    }

    @Test
    fun `network error shows a retryable message and retry resubmits with the same input`() = runTest {
        repository.passwordResult = AuthResult.NetworkError
        vm = createViewModel()
        vm.fillValidForm("pw")
        vm.onLoginClick()
        advanceUntilIdle()

        val message = state.message!!
        assertEquals(MessageKind.NetworkError, message.kind)
        assertTrue(message.canRetry)
        assertEquals("pw", state.password)

        vm.onMessageShown(message.id)
        assertNull(state.message)

        repository.passwordResult = AuthResult.Success(testSession)
        vm.onRetry()
        advanceUntilIdle()
        assertEquals(2, repository.passwordCalls.size)
        assertEquals("pw", repository.passwordCalls.last().password)
        assertTrue(state.isLoggedIn)
    }

    @Test
    fun `server error shows a retryable message`() = runTest {
        repository.passwordResult = AuthResult.ServerError
        vm = createViewModel()
        vm.fillValidForm()
        vm.onLoginClick()
        advanceUntilIdle()
        assertEquals(MessageKind.ServerError, state.message?.kind)
        assertEquals(true, state.message?.canRetry)
    }

    @Test
    fun `no passkey shows a dismissible message and password login stays available`() = runTest {
        repository.passkeyResult = AuthResult.NoPasskey
        vm = createViewModel()
        vm.fillValidForm()
        vm.onPasskeyClick()
        advanceUntilIdle()

        assertEquals(1, repository.passkeyCalls)
        assertEquals(MessageKind.NoPasskey, state.message?.kind)
        assertEquals(false, state.message?.canRetry)
        assertTrue(state.isLoginEnabled)
    }

    @Test
    fun `cancelled passkey shows a cancelled message`() = runTest {
        repository.passkeyResult = AuthResult.Cancelled
        vm = createViewModel()
        vm.onPasskeyClick()
        advanceUntilIdle()
        assertEquals(MessageKind.Cancelled(AuthMethod.PASSKEY), state.message?.kind)
    }

    @Test
    fun `cancelled apple sign in shows a dismissible message`() = runTest {
        repository.appleResult = AuthResult.Cancelled
        vm = createViewModel()
        vm.onSocialClick(SocialProvider.APPLE)
        advanceUntilIdle()
        assertEquals(1, repository.appleCalls)
        assertEquals(MessageKind.Cancelled(AuthMethod.APPLE), state.message?.kind)
    }

    @Test
    fun `failed google sign in shows a failure message`() = runTest {
        repository.googleResult = AuthResult.InvalidCredentials
        vm = createViewModel()
        vm.onSocialClick(SocialProvider.GOOGLE)
        advanceUntilIdle()
        assertEquals(MessageKind.SignInFailed(AuthMethod.GOOGLE), state.message?.kind)
        assertNull(state.formError)
    }

    // endregion

    // region Account linking

    @Test
    fun `account link required shows the dialog and Link calls linkSocialAccount`() = runTest {
        repository.googleResult = AuthResult.AccountLinkRequired("jane@example.com", SocialProvider.GOOGLE)
        vm = createViewModel()
        vm.onSocialClick(SocialProvider.GOOGLE)
        advanceUntilIdle()

        assertEquals(PendingLink("jane@example.com", SocialProvider.GOOGLE), state.pendingLink)
        assertNull(state.loadingMethod)

        vm.onConfirmLink()
        advanceUntilIdle()

        assertEquals(listOf("jane@example.com" to SocialProvider.GOOGLE), repository.linkCalls)
        assertNull(state.pendingLink)
        assertTrue(state.isLoggedIn)
        assertTrue(
            analytics.events.contains(
                AnalyticsEvents.LOGIN_SUCCESS to mapOf(AnalyticsEvents.PARAM_METHOD to "google")
            )
        )
    }

    @Test
    fun `cancelling the link dialog returns to login with no error`() = runTest {
        repository.appleResult = AuthResult.AccountLinkRequired("jane@example.com", SocialProvider.APPLE)
        vm = createViewModel()
        vm.onSocialClick(SocialProvider.APPLE)
        advanceUntilIdle()
        assertNotNull(state.pendingLink)

        vm.onDismissLink()
        advanceUntilIdle()

        assertNull(state.pendingLink)
        assertNull(state.message)
        assertNull(state.formError)
        assertFalse(state.isLoggedIn)
        assertTrue(repository.linkCalls.isEmpty())
    }

    // endregion

    // region Analytics, saved state and security

    @Test
    fun `analytics tracks attempt and failure with method but no personal data`() = runTest {
        repository.passwordResult = AuthResult.InvalidCredentials
        vm = createViewModel()
        vm.fillValidForm("hunter2")
        vm.onLoginClick()
        advanceUntilIdle()
        vm.onForgotPasswordClick()
        vm.onSignUpClick()

        assertEquals(
            listOf(
                AnalyticsEvents.LOGIN_ATTEMPT,
                AnalyticsEvents.LOGIN_FAILURE,
                AnalyticsEvents.FORGOT_PASSWORD_TAPPED,
                AnalyticsEvents.SIGN_UP_TAPPED,
            ),
            analytics.events.map { it.first },
        )
        assertEquals("password", analytics.events[1].second[AnalyticsEvents.PARAM_METHOD])
        val allValues = analytics.events.flatMap { it.second.values }
        assertFalse(allValues.any { it.contains("hunter2") || it.contains("user@example.com") })
    }

    @Test
    fun `password is never written to SavedStateHandle and email is restored`() {
        vm = createViewModel()
        vm.fillValidForm("topsecret")
        vm.onRememberMeChange(true)

        savedStateHandle.keys().forEach { key ->
            assertFalse(savedStateHandle.get<Any?>(key).toString().contains("topsecret"))
        }

        val restored = LoginViewModel(savedStateHandle, repository, tokenStorage, analytics)
        assertEquals("user@example.com", restored.uiState.value.email)
        assertTrue(restored.uiState.value.rememberMe)
        assertEquals("", restored.uiState.value.password)
    }

    @Test
    fun `ui state toString does not leak the password`() {
        vm = createViewModel()
        vm.fillValidForm("topsecret")
        assertFalse(state.toString().contains("topsecret"))
    }

    // endregion
}
