// Copyright 2023 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.welcomeflow.createaccount

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.firstrun.widgets.texts.ToggleSignUpText
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.welcomeflow.WelcomeFlowButtonContainer
import com.neeva.app.welcomeflow.WelcomeFlowContainer

@Composable
fun SignInScreen(
    launchSignUpFlow: () -> Unit,
    onBack: () -> Unit,
) {
    WelcomeFlowContainer(
        onBack = onBack,
        headerText = stringResource(id = R.string.welcomeflow_sign_in_to_neeva)
    ) {
        Column(modifier = it) {
            Spacer(Modifier.height(96.dp))
            WelcomeFlowButtonContainer {
                LoginButton(NeevaUser.SSOProvider.GOOGLE, signup = false)
                Spacer(Modifier.height(18.dp))
                LoginButton(NeevaUser.SSOProvider.OKTA, signup = false)
                Spacer(Modifier.height(18.dp))
                LoginButton(NeevaUser.SSOProvider.MICROSOFT, signup = false)
            }
            Spacer(Modifier.height(32.dp))
            ToggleSignUpText(signup = false, onClick = launchSignUpFlow)
        }
    }
}

@PortraitPreviews
@Composable
fun SignIn_Light_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        SignInScreen(
            launchSignUpFlow = { },
            onBack = { }
        )
    }
}

@PortraitPreviews
@Composable
fun SignIn_Dark_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        SignInScreen(
            launchSignUpFlow = { },
            onBack = { }
        )
    }
}
