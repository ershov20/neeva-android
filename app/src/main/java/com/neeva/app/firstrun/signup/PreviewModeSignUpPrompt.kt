// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.firstrun.signup

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalFirstRunModel
import com.neeva.app.LocalNeevaConstants
import com.neeva.app.R
import com.neeva.app.firstrun.rememberSignInFlowNavModel
import com.neeva.app.ui.LandscapePreviews
import com.neeva.app.ui.LandscapePreviewsDark
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.PortraitPreviewsDark
import com.neeva.app.ui.PreviewCompositionLocals
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun PreviewModeSignUpPrompt(
    query: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val appNavModel = LocalAppNavModel.current
    val firstRunModel = LocalFirstRunModel.current
    val signInFlowNavModel = rememberSignInFlowNavModel()
    val neevaConstants = LocalNeevaConstants.current

    SignUpLandingScreen(
        launchLoginIntent = {
            firstRunModel.getLaunchLoginIntent(context).invoke(it)
            onDismiss()
        },
        onOpenUrl = {
            appNavModel.openUrl(it)
            onDismiss()
        },
        showSignUpWithOther = {
            signInFlowNavModel.navigateToSignUpWithOther()
            onDismiss()
        },
        neevaConstants = neevaConstants,
        primaryLabelString = stringResource(id = R.string.preview_overlay_prompt, query),
        modifier = Modifier.padding(Dimensions.PADDING_LARGE)
    )
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun PreviewModeSignUpPromptPreview() {
    PreviewCompositionLocals {
        NeevaTheme(useDarkTheme = false) {
            Surface(modifier = Modifier.verticalScroll(rememberScrollState())) {
                PreviewModeSignUpPrompt(
                    query = "thing",
                    onDismiss = {}
                )
            }
        }
    }
}

@PortraitPreviewsDark
@LandscapePreviewsDark
@Composable
fun PreviewModeSignUpPromptPreview_Dark() {
    PreviewCompositionLocals {
        NeevaTheme(useDarkTheme = true) {
            Surface(modifier = Modifier.verticalScroll(rememberScrollState())) {
                PreviewModeSignUpPrompt(
                    query = "thing",
                    onDismiss = {}
                )
            }
        }
    }
}
