// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.firstrun.signup

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.neeva.app.LocalNeevaConstants
import com.neeva.app.NeevaConstants
import com.neeva.app.R
import com.neeva.app.firstrun.LaunchLoginIntentParams
import com.neeva.app.firstrun.OnboardingContainer
import com.neeva.app.firstrun.widgets.buttons.OnboardingButton
import com.neeva.app.firstrun.widgets.texts.AcknowledgementText
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
fun SignUpLandingContainer(
    launchLoginIntent: (LaunchLoginIntentParams) -> Unit,
    onOpenUrl: (Uri) -> Unit,
    onClose: () -> Unit,
    navigateToSignIn: () -> Unit,
    showSignUpWithOther: () -> Unit,
    neevaConstants: NeevaConstants = LocalNeevaConstants.current
) {
    OnboardingContainer(
        showBrowser = onClose,
        useSignUpStickyFooter = true, stickyFooterOnClick = navigateToSignIn
    ) { modifier ->
        SignUpLandingScreen(
            launchLoginIntent = launchLoginIntent,
            onOpenUrl = onOpenUrl,
            showSignUpWithOther = showSignUpWithOther,
            neevaConstants = neevaConstants,
            modifier = modifier
        )
    }
}

@Composable
fun SignUpLandingScreen(
    launchLoginIntent: (LaunchLoginIntentParams) -> Unit,
    onOpenUrl: (Uri) -> Unit,
    showSignUpWithOther: () -> Unit,
    neevaConstants: NeevaConstants,
    primaryLabelString: String = stringResource(id = R.string.first_run_intro),
    modifier: Modifier
) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Spacer(modifier = Modifier.height(Dimensions.PADDING_MEDIUM))

        WelcomeHeader(
            primaryLabel = primaryLabelString,
            secondaryLabel = stringResource(id = R.string.first_run_create_your_free_account)
        )

        Spacer(modifier = Modifier.height(Dimensions.PADDING_MEDIUM))

        OnboardingButton(
            signup = true,
            provider = NeevaUser.SSOProvider.GOOGLE,
            launchLoginIntent = launchLoginIntent
        )

        Spacer(modifier = Modifier.height(Dimensions.PADDING_MEDIUM))

        OnboardingButton(text = stringResource(id = R.string.sign_up_other_options)) {
            showSignUpWithOther()
        }

        Spacer(modifier = Modifier.height(Dimensions.PADDING_MEDIUM))

        Box(Modifier.padding(Dimensions.PADDING_MEDIUM)) {
            AcknowledgementText(
                onOpenURL = onOpenUrl,
                appTermsURL = neevaConstants.appTermsURL,
                appPrivacyURL = neevaConstants.appPrivacyURL
            )
        }

        Spacer(modifier = Modifier.height(Dimensions.PADDING_MEDIUM))
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun SignUpLanding_Light_Preview() {
    PreviewCompositionLocals {
        NeevaTheme {
            SignUpLandingContainer(
                launchLoginIntent = {},
                onOpenUrl = {},
                onClose = {},
                navigateToSignIn = {},
                showSignUpWithOther = {},
                neevaConstants = LocalNeevaConstants.current
            )
        }
    }
}

@PortraitPreviewsDark
@LandscapePreviewsDark
@Composable
fun SignUpLanding_Dark_Preview() {
    PreviewCompositionLocals {
        NeevaTheme(useDarkTheme = true) {
            SignUpLandingContainer(
                launchLoginIntent = {},
                onOpenUrl = {},
                onClose = {},
                navigateToSignIn = {},
                showSignUpWithOther = {},
                neevaConstants = LocalNeevaConstants.current
            )
        }
    }
}
