// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.firstrun.widgets.buttons

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neeva.app.firstrun.LaunchLoginIntentParams
import com.neeva.app.userdata.NeevaUser

@Composable
fun ToggleOnboardingButtons(
    signup: Boolean,
    emailProvided: String,
    launchLoginIntent: (LaunchLoginIntentParams) -> Unit
) {
    OnboardingButton(
        emailProvided = emailProvided,
        signup = signup,
        provider = NeevaUser.SSOProvider.GOOGLE,
        launchLoginIntent = launchLoginIntent
    )

    Spacer(modifier = Modifier.height(20.dp))

    OnboardingButton(
        emailProvided = emailProvided,
        signup = signup,
        provider = NeevaUser.SSOProvider.MICROSOFT,
        launchLoginIntent = launchLoginIntent
    )
}
