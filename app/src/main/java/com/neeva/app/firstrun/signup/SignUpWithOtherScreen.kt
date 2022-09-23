// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.firstrun.signup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.neeva.app.R
import com.neeva.app.firstrun.LaunchLoginIntentParams
import com.neeva.app.firstrun.OnboardingContainer
import com.neeva.app.firstrun.widgets.OrSeparator
import com.neeva.app.firstrun.widgets.buttons.OnboardingButton
import com.neeva.app.firstrun.widgets.buttons.ToggleOnboardingButtons
import com.neeva.app.firstrun.widgets.textfields.ClearFocusOnDismissTextField
import com.neeva.app.firstrun.widgets.textfields.PasswordTextField
import com.neeva.app.firstrun.widgets.texts.BadPasswordText
import com.neeva.app.firstrun.widgets.texts.WelcomeHeader
import com.neeva.app.ui.LandscapePreviews
import com.neeva.app.ui.LandscapePreviewsDark
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.PortraitPreviewsDark
import com.neeva.app.ui.PreviewCompositionLocals
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.userdata.NeevaUser

@Composable
fun SignUpWithOtherContainer(
    launchLoginIntent: (LaunchLoginIntentParams) -> Unit,
    onClose: () -> Unit,
    navigateToSignIn: () -> Unit
) {
    OnboardingContainer(
        showBrowser = onClose,
        useSignUpStickyFooter = true, stickyFooterOnClick = navigateToSignIn
    ) { modifier ->
        SignUpWithOtherScreen(
            launchLoginIntent = launchLoginIntent,
            modifier = modifier
        )
    }
}

@Composable
fun SignUpWithOtherScreen(
    launchLoginIntent: (LaunchLoginIntentParams) -> Unit,
    modifier: Modifier
) {
    val email = rememberSaveable { mutableStateOf("") }
    val password = rememberSaveable { mutableStateOf("") }

    Column(
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.height(Dimensions.PADDING_MEDIUM))

        WelcomeHeader(
            primaryLabel = stringResource(id = R.string.first_run_intro),
            secondaryLabel = stringResource(id = R.string.first_run_create_your_free_account)
        )

        Spacer(modifier = Modifier.height(Dimensions.PADDING_MEDIUM))

        ClearFocusOnDismissTextField(
            text = email.value,
            onTextChanged = { email.value = it },
            label = stringResource(id = R.string.email_label)
        )

        Spacer(modifier = Modifier.height(Dimensions.PADDING_MEDIUM))

        PasswordTextField(
            text = password.value,
            onTextChanged = { password.value = it },
            label = stringResource(id = R.string.password_label)
        )

        BadPasswordText(password = password.value)

        OnboardingButton(
            emailProvided = email.value,
            passwordProvided = password.value,
            signup = true,
            provider = NeevaUser.SSOProvider.OKTA,
            launchLoginIntent = launchLoginIntent
        )

        Spacer(modifier = Modifier.height(Dimensions.PADDING_MEDIUM))

        OrSeparator()

        Spacer(modifier = Modifier.height(Dimensions.PADDING_MEDIUM))

        ToggleOnboardingButtons(
            signup = true,
            emailProvided = email.value,
            launchLoginIntent = launchLoginIntent
        )

        Spacer(modifier = Modifier.height(Dimensions.PADDING_MEDIUM))
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun SignUpOther_Light_Preview() {
    PreviewCompositionLocals {
        NeevaTheme {
            SignUpWithOtherContainer(
                launchLoginIntent = {},
                onClose = {},
                navigateToSignIn = {}
            )
        }
    }
}

@PortraitPreviewsDark
@LandscapePreviewsDark
@Composable
fun SignUpOther_Dark_Preview() {
    PreviewCompositionLocals {
        NeevaTheme(useDarkTheme = true) {
            SignUpWithOtherContainer(
                launchLoginIntent = {},
                onClose = {},
                navigateToSignIn = {}
            )
        }
    }
}
