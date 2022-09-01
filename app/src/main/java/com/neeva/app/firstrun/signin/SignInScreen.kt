// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.firstrun.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.firstrun.LaunchLoginIntentParams
import com.neeva.app.firstrun.OnboardingContainer
import com.neeva.app.firstrun.widgets.OrSeparator
import com.neeva.app.firstrun.widgets.buttons.OnboardingButton
import com.neeva.app.firstrun.widgets.buttons.ToggleOnboardingButtons
import com.neeva.app.firstrun.widgets.textfields.OnboardingTextField
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
fun SignInScreenContainer(
    launchLoginIntent: (LaunchLoginIntentParams) -> Unit,
    onClose: () -> Unit,
    navigateToSignUp: () -> Unit
) {
    OnboardingContainer(
        showBrowser = onClose,
        useSignUpStickyFooter = false, stickyFooterOnClick = navigateToSignUp
    ) { modifier ->
        SignInScreen(
            launchLoginIntent = launchLoginIntent,
            modifier = modifier
        )
    }
}

@Composable
fun SignInScreen(
    launchLoginIntent: (LaunchLoginIntentParams) -> Unit,
    modifier: Modifier
) {
    val email = rememberSaveable { mutableStateOf("") }

    Column(
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.padding(Dimensions.PADDING_HUGE))

        WelcomeHeader(
            primaryLabel = stringResource(id = R.string.sign_in)
        )

        Spacer(modifier = Modifier.height(32.dp))

        OnboardingTextField(
            text = email.value,
            onTextChanged = { email.value = it },
            label = stringResource(id = R.string.email_label),
        )

        Spacer(modifier = Modifier.height(28.dp))

        OnboardingButton(
            emailProvided = email.value,
            signup = false,
            provider = NeevaUser.SSOProvider.OKTA,
            launchLoginIntent = launchLoginIntent
        )

        Spacer(modifier = Modifier.height(16.dp))

        OrSeparator()

        Spacer(modifier = Modifier.height(16.dp))

        ToggleOnboardingButtons(
            signup = false,
            emailProvided = email.value,
            launchLoginIntent = launchLoginIntent
        )

        Spacer(modifier = Modifier.padding(Dimensions.PADDING_HUGE))
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun SignInScreen_Light_Preview() {
    PreviewCompositionLocals {
        NeevaTheme {
            SignInScreenContainer(
                launchLoginIntent = {},
                onClose = {},
                navigateToSignUp = {}
            )
        }
    }
}

@PortraitPreviewsDark
@LandscapePreviewsDark
@Composable
fun SignInScreen_Dark_Preview() {
    PreviewCompositionLocals {
        NeevaTheme(useDarkTheme = true) {
            SignInScreenContainer(
                launchLoginIntent = {},
                onClose = {},
                navigateToSignUp = {}
            )
        }
    }
}
