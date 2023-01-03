// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.welcomeflow.createaccount

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.firstrun.widgets.texts.EmailPromoCheckbox
import com.neeva.app.firstrun.widgets.texts.ToggleSignUpText
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.welcomeflow.SecondaryWelcomeFlowButton
import com.neeva.app.welcomeflow.WelcomeFlowButtonContainer
import com.neeva.app.welcomeflow.WelcomeFlowContainer

@Composable
fun CreateAccountScreen(
    launchSignUpFlow: () -> Unit,
    onShowOtherSignUpOptions: (() -> Unit)? = null,
    onBack: () -> Unit,
) {
    WelcomeFlowContainer(
        onBack = onBack,
        headerText = stringResource(id = R.string.welcomeflow_create_your_account)
    ) {
        Column(modifier = it) {
            Spacer(Modifier.height(96.dp))
            if (onShowOtherSignUpOptions != null) {
                SignUpWithGoogleButtons(onOtherOptions = onShowOtherSignUpOptions)
            } else {
                SignUpWithOtherButtons()
            }
            Spacer(Modifier.height(32.dp))
            EmailPromoCheckbox(Modifier.padding(Dimensions.PADDING_MEDIUM))
            Spacer(Modifier.size(Dimensions.PADDING_LARGE))
            ToggleSignUpText(signup = true, onClick = launchSignUpFlow)
        }
    }
}

@Composable
fun SignUpWithGoogleButtons(onOtherOptions: () -> Unit) {
    WelcomeFlowButtonContainer {
        LoginButton(NeevaUser.SSOProvider.GOOGLE, signup = true)
        Spacer(Modifier.height(18.dp))
        SecondaryWelcomeFlowButton(text = stringResource(id = R.string.sign_up_other_options)) {
            onOtherOptions()
        }
    }
}

@Composable
fun SignUpWithOtherButtons() {
    WelcomeFlowButtonContainer {
        LoginButton(NeevaUser.SSOProvider.GOOGLE, signup = true)
        Spacer(Modifier.height(18.dp))
        LoginButton(NeevaUser.SSOProvider.OKTA, signup = true)
        Spacer(Modifier.height(18.dp))
        LoginButton(NeevaUser.SSOProvider.MICROSOFT, signup = true)
    }
}

@PortraitPreviews
@Composable
fun CreateAccount_GoogleSignIn_Light_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        CreateAccountScreen(
            launchSignUpFlow = { },
            onShowOtherSignUpOptions = { },
            onBack = { }
        )
    }
}

@PortraitPreviews
@Composable
fun CreateAccount_GoogleSignIn_Dark_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        CreateAccountScreen(
            launchSignUpFlow = { },
            onShowOtherSignUpOptions = { },
            onBack = { }
        )
    }
}

@PortraitPreviews
@Composable
fun CreateAccount_OtherSignIn_Light_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        CreateAccountScreen(
            launchSignUpFlow = { },
            onBack = { }
        )
    }
}

@PortraitPreviews
@Composable
fun CreateAccount_OtherSignIn_Dark_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        CreateAccountScreen(
            launchSignUpFlow = { },
            onBack = { }
        )
    }
}
