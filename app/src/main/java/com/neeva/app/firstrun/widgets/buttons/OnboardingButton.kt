// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.firstrun.widgets.buttons

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalFirstRunModel
import com.neeva.app.LocalIsDarkTheme
import com.neeva.app.R
import com.neeva.app.firstrun.LaunchLoginIntentParams
import com.neeva.app.ui.TwoBooleanPreviewContainer
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.getClickableAlpha
import com.neeva.app.ui.widgets.RowActionIcon
import com.neeva.app.ui.widgets.RowActionIconParams
import com.neeva.app.ui.widgets.icons.SSOProviderImage
import com.neeva.app.userdata.NeevaUser

// TODO(kobec): Make it styled differently for Okta buttons
@Composable
fun OnboardingButton(
    emailProvided: String? = null,
    passwordProvided: String? = null,
    signup: Boolean,
    provider: NeevaUser.SSOProvider,
    launchLoginIntent: (LaunchLoginIntentParams) -> Unit,
    enabled: Boolean = true
) {
    val firstRunModel = LocalFirstRunModel.current
    val appNavModel = LocalAppNavModel.current
    val context = LocalContext.current

    OnboardingButton(
        emailProvided = emailProvided,
        passwordProvided = passwordProvided,
        signup = signup,
        provider = provider,
        launchLoginIntent = launchLoginIntent,
        onActivityResult = { result ->
            firstRunModel.handleLoginActivityResult(context, result) { appNavModel.openUrl(it) }
        },
        enabled = enabled
    )
}

@Composable
fun OnboardingButton(
    emailProvided: String? = null,
    passwordProvided: String? = null,
    signup: Boolean,
    provider: NeevaUser.SSOProvider,
    launchLoginIntent: (LaunchLoginIntentParams) -> Unit,
    onActivityResult: (ActivityResult) -> Unit,
    enabled: Boolean = true
) {
    val resultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        onActivityResult
    )
    val onClick = {
        launchLoginIntent(
            LaunchLoginIntentParams(
                provider = provider,
                signup = signup,
                emailProvided = emailProvided,
                passwordProvided = passwordProvided,
                resultLauncher = resultLauncher
            )
        )
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

@Preview("OnboardingButton Light LTR 1x scale", locale = "en")
@Preview("OnboardingButton Light 2x scale", locale = "en", fontScale = 2.0f)
@Preview("OnboardingButton Light 1x scale", locale = "he")
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

@Preview("OnboardingButton Dark LTR 1x scale", locale = "en")
@Preview("OnboardingButton Dark 2x scale", locale = "en", fontScale = 2.0f)
@Preview("OnboardingButton Dark 1x scale", locale = "he")
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

@Preview("NeevaOnboardingButton LTR 1x scale", locale = "en")
@Preview("NeevaOnboardingButton 2x scale", locale = "en", fontScale = 2.0f)
@Preview("NeevaOnboardingButton 1x scale", locale = "he")
@Composable
fun NeevaOnboardingButtonPreview_Light() {
    TwoBooleanPreviewContainer { signup, isEnabled ->
        OnboardingButton(
            signup = signup,
            enabled = isEnabled,
            provider = NeevaUser.SSOProvider.OKTA,
            launchLoginIntent = {},
            onActivityResult = {}
        )
    }
}
