// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.firstrun.widgets.buttons

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalFirstRunModel
import com.neeva.app.LocalIsDarkTheme
import com.neeva.app.R
import com.neeva.app.firstrun.LaunchLoginFlowParams
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.PortraitPreviewsDark
import com.neeva.app.ui.TwoBooleanPreviewContainer
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.getClickableAlpha
import com.neeva.app.ui.widgets.RowActionIcon
import com.neeva.app.ui.widgets.RowActionIconParams
import com.neeva.app.ui.widgets.icons.SSOProviderImage
import com.neeva.app.userdata.NeevaUser

fun interface OnboardingButtonListener {
    fun onLoginFlowLaunched()
}

val LocalOnboardingButtonListener = compositionLocalOf<OnboardingButtonListener?> { null }

// TODO(kobec): Make it styled differently for Okta buttons
@Composable
fun OnboardingButton(
    emailProvided: String? = null,
    passwordProvided: String? = null,
    signup: Boolean,
    provider: NeevaUser.SSOProvider,
    enabled: Boolean = true
) {
    val firstRunModel = LocalFirstRunModel.current
    val appNavModel = LocalAppNavModel.current
    val context = LocalContext.current
    val onboardingButtonListener = LocalOnboardingButtonListener.current

    OnboardingButton(
        emailProvided = emailProvided,
        passwordProvided = passwordProvided,
        signup = signup,
        provider = provider,
        onLaunchLoginFlow = { launcher, params ->
            firstRunModel.launchLoginFlow(context, params, launcher)
            onboardingButtonListener?.onLoginFlowLaunched()
        },
        onLaunchActivityResult = { result, params ->
            firstRunModel.handleLoginActivityResult(context, result, params) {
                appNavModel.openUrlInNewTab(it)
            }
        },
        enabled = enabled
    )
}

@Composable
private fun OnboardingButton(
    emailProvided: String? = null,
    passwordProvided: String? = null,
    signup: Boolean,
    provider: NeevaUser.SSOProvider,
    onLaunchLoginFlow: (ActivityResultLauncher<Intent>, LaunchLoginFlowParams) -> Unit,
    onLaunchActivityResult: (ActivityResult, LaunchLoginFlowParams) -> Unit,
    enabled: Boolean = true
) {
    val launchLoginFlowParams = LaunchLoginFlowParams(
        provider = provider,
        signup = signup,
        emailProvided = emailProvided,
        passwordProvided = passwordProvided
    )

    val resultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        onLaunchActivityResult(result, launchLoginFlowParams)
    }

    val onClick = {
        onLaunchLoginFlow(resultLauncher, launchLoginFlowParams)
    }

    when (provider) {
        NeevaUser.SSOProvider.OKTA -> {
            NeevaOnboardingButton(
                text = getSSOProviderOnboardingText(NeevaUser.SSOProvider.OKTA, signup),
                signup = signup,
                onClick = onClick
            )
        }
        else -> {
            OnboardingButton(
                text = getSSOProviderOnboardingText(provider, signup),
                enabled = enabled,
                startComposable = { SSOProviderImage(ssoProvider = provider) },
                onClick = onClick
            )
        }
    }
}

@Composable
fun getSSOProviderOnboardingText(provider: NeevaUser.SSOProvider, signup: Boolean): String {
    when (provider) {
        NeevaUser.SSOProvider.MICROSOFT ->
            return if (signup) {
                stringResource(R.string.sign_up_with_microsoft)
            } else {
                stringResource(R.string.sign_in_with_microsoft)
            }

        NeevaUser.SSOProvider.GOOGLE ->
            return if (signup) {
                stringResource(R.string.sign_up_with_google)
            } else {
                stringResource(R.string.sign_in_with_google)
            }

        NeevaUser.SSOProvider.OKTA ->
            return if (signup) {
                stringResource(R.string.sign_up_with_okta)
            } else {
                stringResource(R.string.sign_in_with_okta)
            }

        else -> throw IllegalStateException("Unsupported SSO Provider!")
    }
}

/** Standardized button used for Onboarding. */
@Composable
fun OnboardingButton(
    text: String,
    enabled: Boolean = true,
    startComposable: @Composable (() -> Unit)? = null,
    endComposable: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    val backgroundColor = if (LocalIsDarkTheme.current) {
        MaterialTheme.colorScheme.background
    } else {
        MaterialTheme.colorScheme.onPrimary
    }

    // Currently in the API: When a button border is set, button elevation is discarded.
    // In dark mode, we want it to have no elevation and a border
    // In light mode, we want it to have elevation but no border.
    // source: https://github.com/neevaco/neeva-android/pull/498#discussion_r843428796
    val border = if (LocalIsDarkTheme.current) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    } else {
        null
    }

    ElevatedButton(
        onClick = onClick,
        enabled = enabled,
        border = border,
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(36.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(getClickableAlpha(enabled))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp)
        ) {
            startComposable?.let {
                it()
                Spacer(modifier = Modifier.width(Dimensions.PADDING_LARGE))
            }

            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            endComposable?.let {
                Spacer(modifier = Modifier.width(Dimensions.PADDING_LARGE))
                it()
            }
        }
    }
}

@Composable
fun NeevaOnboardingButton(
    enabled: Boolean = true,
    text: String,
    signup: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(36.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(getClickableAlpha(enabled))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp)
        ) {
            if (signup) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_neeva_logo),
                    contentDescription = null,
                    modifier = Modifier.size(Dimensions.SIZE_ICON_MEDIUM)
                )
                Spacer(modifier = Modifier.width(Dimensions.PADDING_LARGE))
            }

            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium
            )

            if (!signup) {
                Spacer(modifier = Modifier.width(Dimensions.PADDING_LARGE))
                RowActionIcon(
                    actionType = RowActionIconParams.ActionType.FORWARD
                )
            }
        }
    }
}

@PortraitPreviews
@Composable
fun OnboardingButtonPreview_Light() {
    TwoBooleanPreviewContainer(useDarkTheme = false) { hasStartComposable, isEnabled ->
        val startComposable = @Composable {
            SSOProviderImage(NeevaUser.SSOProvider.GOOGLE)
        }
        OnboardingButton(
            enabled = isEnabled,
            text = stringResource(R.string.sign_in_with_google),
            startComposable = startComposable.takeIf { hasStartComposable }
        ) {}
    }
}

@PortraitPreviewsDark
@Composable
fun OnboardingButtonPreview_Dark() {
    TwoBooleanPreviewContainer(useDarkTheme = true) { hasStartComposable, isEnabled ->
        val startComposable = @Composable {
            SSOProviderImage(NeevaUser.SSOProvider.GOOGLE)
        }
        OnboardingButton(
            enabled = isEnabled,
            text = stringResource(R.string.sign_in_with_google),
            startComposable = startComposable.takeIf { hasStartComposable }
        ) {}
    }
}

@PortraitPreviews
@Composable
fun NeevaOnboardingButtonPreview_LightSignup() {
    OneBooleanPreviewContainer { enabled ->
        OnboardingButton(
            signup = true,
            enabled = enabled,
            provider = NeevaUser.SSOProvider.OKTA,
            onLaunchLoginFlow = { _, _ -> },
            onLaunchActivityResult = { _, _ -> }
        )
    }
}

@PortraitPreviews
@Composable
fun NeevaOnboardingButtonPreview_LightSignin() {
    OneBooleanPreviewContainer { enabled ->
        OnboardingButton(
            signup = false,
            enabled = enabled,
            provider = NeevaUser.SSOProvider.OKTA,
            onLaunchLoginFlow = { _, _ -> },
            onLaunchActivityResult = { _, _ -> }
        )
    }
}

@PortraitPreviewsDark
@Composable
fun NeevaOnboardingButtonPreview_DarkSignup() {
    OneBooleanPreviewContainer(useDarkTheme = true) { enabled ->
        OnboardingButton(
            signup = true,
            enabled = enabled,
            provider = NeevaUser.SSOProvider.OKTA,
            onLaunchLoginFlow = { _, _ -> },
            onLaunchActivityResult = { _, _ -> }
        )
    }
}

@PortraitPreviewsDark
@Composable
fun NeevaOnboardingButtonPreview_DarkSignin() {
    OneBooleanPreviewContainer(useDarkTheme = true) { enabled ->
        OnboardingButton(
            signup = false,
            enabled = enabled,
            provider = NeevaUser.SSOProvider.OKTA,
            onLaunchLoginFlow = { _, _ -> },
            onLaunchActivityResult = { _, _ -> }
        )
    }
}
