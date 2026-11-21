package com.example.myapplication.ui.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.R
import com.example.myapplication.data.auth.SocialProvider
import com.example.myapplication.ui.theme.AppDisabledContent
import com.example.myapplication.ui.theme.MyApplicationTheme

private val ScreenPadding = 24.dp
private val FieldShape = RoundedCornerShape(12.dp)
private val PillShape = RoundedCornerShape(24.dp)
private val MinTouchTarget = 48.dp

/** Stateful entry point: wires [LoginViewModel] to [LoginScreen]. */
@Composable
fun LoginRoute(
    onLoginSuccess: () -> Unit,
    onForgotPassword: (email: String) -> Unit,
    onSignUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = viewModel(factory = LoginViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnLoginSuccess by rememberUpdatedState(onLoginSuccess)

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) {
            viewModel.onLoginSuccessHandled()
            currentOnLoginSuccess()
        }
    }

    LoginScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onEmailFocusChange = viewModel::onEmailFocusChange,
        onPasswordChange = viewModel::onPasswordChange,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onRememberMeChange = viewModel::onRememberMeChange,
        onForgotPasswordClick = {
            viewModel.onForgotPasswordClick()
            onForgotPassword(state.email.trim())
        },
        onLoginClick = viewModel::onLoginClick,
        onPasskeyClick = viewModel::onPasskeyClick,
        onSocialClick = viewModel::onSocialClick,
        onSignUpClick = {
            viewModel.onSignUpClick()
            onSignUp()
        },
        onConfirmLink = viewModel::onConfirmLink,
        onDismissLink = viewModel::onDismissLink,
        onMessageShown = viewModel::onMessageShown,
        onRetry = viewModel::onRetry,
        modifier = modifier,
    )
}

/** Stateless "Welcome back" login screen (SCRUM-1). */
@Composable
fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onEmailFocusChange: (Boolean) -> Unit,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onRememberMeChange: (Boolean) -> Unit,
    onForgotPasswordClick: () -> Unit,
    onLoginClick: () -> Unit,
    onPasskeyClick: () -> Unit,
    onSocialClick: (SocialProvider) -> Unit,
    onSignUpClick: () -> Unit,
    onConfirmLink: () -> Unit,
    onDismissLink: () -> Unit,
    onMessageShown: (Long) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val message = state.message
    val messageText = message?.let { messageText(it.kind) }
    val retryLabel = stringResource(R.string.login_retry)

    LaunchedEffect(message?.id) {
        if (message == null || messageText == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = messageText,
            actionLabel = if (message.canRetry) retryLabel else null,
            withDismissAction = true,
            duration = if (message.canRetry) SnackbarDuration.Long else SnackbarDuration.Short,
        )
        onMessageShown(message.id)
        if (result == SnackbarResult.ActionPerformed) onRetry()
    }

    state.pendingLink?.let { link ->
        LinkAccountDialog(link = link, onConfirm = onConfirmLink, onDismiss = onDismissLink)
    }

    val colors = MaterialTheme.colorScheme
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                // Faint indigo glow at the top of the screen.
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(colors.primary.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width / 2f, 0f),
                        radius = size.width * 0.9f,
                    )
                )
            },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            HeaderIcon()
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.login_title),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.login_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(32.dp))
            EmailField(state, onEmailChange, onEmailFocusChange)
            Spacer(Modifier.height(16.dp))
            PasswordField(state, onPasswordChange, onTogglePasswordVisibility, onLoginClick)

            Spacer(Modifier.height(12.dp))
            RememberMeRow(
                rememberMe = state.rememberMe,
                onRememberMeChange = onRememberMeChange,
                onForgotPasswordClick = onForgotPasswordClick,
            )

            state.formError?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = when (error) {
                        FormError.InvalidCredentials -> stringResource(R.string.login_error_invalid_credentials)
                        is FormError.LockedOut -> pluralStringResource(
                            R.plurals.login_error_locked_out,
                            error.retryAfterMinutes,
                            error.retryAfterMinutes,
                        )
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            Spacer(Modifier.height(16.dp))
            LoginButton(state = state, onClick = onLoginClick)
            Spacer(Modifier.height(12.dp))
            PasskeyButton(
                isLoading = state.loadingMethod == AuthMethod.PASSKEY,
                // Disabled while a different sign-in is running, so taps aren't silently ignored.
                enabled = !state.isLoading || state.loadingMethod == AuthMethod.PASSKEY,
                onClick = { if (!state.isLoading) onPasskeyClick() },
            )

            Spacer(Modifier.height(24.dp))
            OrDivider()
            Spacer(Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SocialButton(
                    label = stringResource(R.string.login_google),
                    description = stringResource(R.string.login_continue_with_google),
                    iconRes = R.drawable.ic_google_logo,
                    tintIcon = false,
                    isLoading = state.loadingMethod == AuthMethod.GOOGLE,
                    enabled = !state.isLoading || state.loadingMethod == AuthMethod.GOOGLE,
                    onClick = { if (!state.isLoading) onSocialClick(SocialProvider.GOOGLE) },
                    modifier = Modifier.weight(1f),
                )
                SocialButton(
                    label = stringResource(R.string.login_apple),
                    description = stringResource(R.string.login_continue_with_apple),
                    iconRes = R.drawable.ic_apple_logo,
                    tintIcon = true,
                    isLoading = state.loadingMethod == AuthMethod.APPLE,
                    enabled = !state.isLoading || state.loadingMethod == AuthMethod.APPLE,
                    onClick = { if (!state.isLoading) onSocialClick(SocialProvider.APPLE) },
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(40.dp))
            SignUpRow(onSignUpClick = onSignUpClick)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun messageText(kind: MessageKind): String = when (kind) {
    MessageKind.NoPasskey -> stringResource(R.string.login_error_no_passkey)
    MessageKind.NetworkError -> stringResource(R.string.login_error_network)
    MessageKind.ServerError -> stringResource(R.string.login_error_server)
    is MessageKind.Cancelled -> when (kind.method) {
        AuthMethod.PASSKEY, AuthMethod.PASSWORD -> stringResource(R.string.login_passkey_cancelled)
        AuthMethod.GOOGLE -> stringResource(R.string.login_social_cancelled, stringResource(R.string.login_google))
        AuthMethod.APPLE -> stringResource(R.string.login_social_cancelled, stringResource(R.string.login_apple))
    }
    is MessageKind.SignInFailed -> stringResource(R.string.login_social_failed, methodLabel(kind.method))
}

@Composable
private fun methodLabel(method: AuthMethod): String = when (method) {
    AuthMethod.GOOGLE -> stringResource(R.string.login_google)
    AuthMethod.APPLE -> stringResource(R.string.login_apple)
    AuthMethod.PASSKEY, AuthMethod.PASSWORD -> stringResource(R.string.login_passkey)
}

@Composable
private fun HeaderIcon() {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .size(56.dp)
            .drawBehind {
                // Soft indigo glow around the tile.
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.primary.copy(alpha = 0.35f), Color.Transparent),
                        center = center,
                        radius = size.minDimension,
                    ),
                    radius = size.minDimension,
                )
            }
            .background(colors.surfaceContainerHigh, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_shield_lock),
            contentDescription = stringResource(R.string.login_secure_account_icon),
            tint = colors.primary,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun fieldColors() = MaterialTheme.colorScheme.let { c ->
    OutlinedTextFieldDefaults.colors(
        focusedContainerColor = c.surfaceContainerHigh,
        unfocusedContainerColor = c.surfaceContainerHigh,
        disabledContainerColor = c.surfaceContainerHigh,
        errorContainerColor = c.surfaceContainerHigh,
        focusedBorderColor = c.primary,
        unfocusedBorderColor = Color.Transparent,
        errorBorderColor = c.error,
        focusedTextColor = c.onSurface,
        unfocusedTextColor = c.onSurface,
        focusedPlaceholderColor = c.outline,
        unfocusedPlaceholderColor = c.outline,
        errorPlaceholderColor = c.outline,
        focusedLeadingIconColor = c.outline,
        unfocusedLeadingIconColor = c.outline,
        errorLeadingIconColor = c.outline,
        focusedTrailingIconColor = c.outline,
        unfocusedTrailingIconColor = c.outline,
        errorTrailingIconColor = c.outline,
        cursorColor = c.primary,
    )
}

@Composable
private fun EmailField(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onEmailFocusChange: (Boolean) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val errorText = state.emailError?.let { stringResource(R.string.login_email_error_invalid) }
    FieldLabel(stringResource(R.string.login_email_label))
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = state.email,
        onValueChange = onEmailChange,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { onEmailFocusChange(it.isFocused) }
            .semantics {
                contentType = ContentType.EmailAddress + ContentType.Username
                if (errorText != null) error(errorText)
            },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = { Text(stringResource(R.string.login_email_placeholder)) },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_mail),
                contentDescription = null, // Decorative: the field is labelled by "Email address".
                modifier = Modifier.size(20.dp),
            )
        },
        isError = errorText != null,
        supportingText = errorText?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next,
        ),
        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
        shape = FieldShape,
        colors = fieldColors(),
    )
}

@Composable
private fun PasswordField(
    state: LoginUiState,
    onPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onSubmit: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    FieldLabel(stringResource(R.string.login_password_label))
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = state.password,
        onValueChange = onPasswordChange,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentType = ContentType.Password },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = { Text(stringResource(R.string.login_password_placeholder)) },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_lock),
                contentDescription = null, // Decorative: the field is labelled by "Password".
                modifier = Modifier.size(20.dp),
            )
        },
        trailingIcon = {
            // IconButton gives a 48dp touch target.
            IconButton(onClick = onTogglePasswordVisibility) {
                Icon(
                    painter = painterResource(
                        if (state.isPasswordVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility
                    ),
                    contentDescription = stringResource(
                        if (state.isPasswordVisible) R.string.login_hide_password else R.string.login_show_password
                    ),
                    modifier = Modifier.size(24.dp),
                )
            }
        },
        isError = state.formError != null,
        visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = {
            focusManager.clearFocus() // Hide the keyboard so the result (or the disabled button) is visible.
            if (state.isLoginEnabled) onSubmit()
        }),
        shape = FieldShape,
        colors = fieldColors(),
    )
}

@Composable
private fun RememberMeRow(
    rememberMe: Boolean,
    onRememberMeChange: (Boolean) -> Unit,
    onForgotPasswordClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // The whole row (checkbox + label) is one toggleable target.
        Row(
            modifier = Modifier
                .weight(1f, fill = false)
                .heightIn(min = MinTouchTarget)
                .toggleable(value = rememberMe, role = Role.Checkbox, onValueChange = onRememberMeChange),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = rememberMe,
                onCheckedChange = null,
                colors = CheckboxDefaults.colors(
                    checkedColor = colors.primary,
                    uncheckedColor = colors.outline,
                    checkmarkColor = colors.onPrimary,
                ),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.login_remember_me),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        TextButton(
            onClick = onForgotPasswordClick,
            modifier = Modifier.heightIn(min = MinTouchTarget),
        ) {
            Text(
                text = stringResource(R.string.login_forgot_password),
                style = MaterialTheme.typography.labelLarge,
                color = colors.primary,
            )
        }
    }
}

@Composable
private fun LoginButton(state: LoginUiState, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val isLoading = state.loadingMethod == AuthMethod.PASSWORD
    val loadingDescription = stringResource(R.string.login_in_progress)
    Button(
        // While loading, the button keeps its enabled look but ignores taps (blocks repeat submits).
        onClick = { if (state.isLoginEnabled) onClick() },
        enabled = state.isLoginEnabled || isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .semantics { if (isLoading) stateDescription = loadingDescription },
        shape = FieldShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.surfaceContainer,
            disabledContentColor = AppDisabledContent,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = colors.onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text = stringResource(R.string.login_button), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(R.drawable.ic_arrow_forward),
                contentDescription = null, // Decorative: the button is labelled "Log in".
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun PasskeyButton(isLoading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val loadingDescription = stringResource(R.string.login_in_progress)
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .heightIn(min = MinTouchTarget)
            .semantics { if (isLoading) stateDescription = loadingDescription },
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.surfaceContainerLow,
            contentColor = colors.onSurfaceVariant,
        ),
        contentPadding = ButtonDefaults.ContentPadding,
    ) {
        if (isLoading) {
            CircularProgressIndicator(Modifier.size(18.dp), color = colors.primary, strokeWidth = 2.dp)
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_fingerprint),
                contentDescription = null, // Decorative: the button text describes the action.
                tint = colors.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(text = stringResource(R.string.login_passkey), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun OrDivider() {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(Modifier.weight(1f), color = colors.outlineVariant)
        Text(
            text = stringResource(R.string.login_or_continue_with),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.outline,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        HorizontalDivider(Modifier.weight(1f), color = colors.outlineVariant)
    }
}

@Composable
private fun SocialButton(
    label: String,
    description: String,
    iconRes: Int,
    tintIcon: Boolean,
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val loadingDescription = stringResource(R.string.login_in_progress)
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = MinTouchTarget)
            // Replace the merged label with the description. Don't use clearAndSetSemantics here:
            // on the Button's own modifier it also clears its click action, so TalkBack can't press it.
            .semantics {
                contentDescription = description
                if (isLoading) stateDescription = loadingDescription
            },
        shape = FieldShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.surfaceContainerLow,
            contentColor = colors.onSurface,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(Modifier.size(20.dp), color = colors.primary, strokeWidth = 2.dp)
        } else if (tintIcon) {
            Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(20.dp))
        } else {
            Image(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clearAndSetSemantics {}, // Read via the button's description.
        )
    }
}

@Composable
private fun SignUpRow(onSignUpClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val signUpDescription = stringResource(R.string.login_sign_up_description)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(R.string.login_no_account),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
        )
        TextButton(
            onClick = onSignUpClick,
            modifier = Modifier
                .heightIn(min = MinTouchTarget)
                .semantics { contentDescription = signUpDescription },
        ) {
            Text(
                text = stringResource(R.string.login_sign_up),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = colors.primary,
                modifier = Modifier.clearAndSetSemantics {}, // Read via the button's description.
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.ic_open_in_new),
                contentDescription = null, // Covered by the button's description.
                tint = colors.primary,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun LinkAccountDialog(link: PendingLink, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val providerName = when (link.provider) {
        SocialProvider.GOOGLE -> stringResource(R.string.login_google)
        SocialProvider.APPLE -> stringResource(R.string.login_apple)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.login_link_title)) },
        text = { Text(stringResource(R.string.login_link_message, link.email, providerName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.login_link_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.login_link_cancel)) }
        },
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF11141E, heightDp = 900)
@Composable
private fun LoginScreenPreview() {
    MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        LoginScreen(
            state = LoginUiState(),
            onEmailChange = {},
            onEmailFocusChange = {},
            onPasswordChange = {},
            onTogglePasswordVisibility = {},
            onRememberMeChange = {},
            onForgotPasswordClick = {},
            onLoginClick = {},
            onPasskeyClick = {},
            onSocialClick = {},
            onSignUpClick = {},
            onConfirmLink = {},
            onDismissLink = {},
            onMessageShown = {},
            onRetry = {},
        )
    }
}
