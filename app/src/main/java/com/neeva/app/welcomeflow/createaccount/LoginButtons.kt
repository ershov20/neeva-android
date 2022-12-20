// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.welcomeflow.createaccount

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalFirstRunModel
import com.neeva.app.R
import com.neeva.app.firstrun.LaunchLoginFlowParams
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.userdata.NeevaUser

@Composable
fun LoginButton(provider: NeevaUser.SSOProvider) {
    val firstRunModel = LocalFirstRunModel.current
    val appNavModel = LocalAppNavModel.current
    val context = LocalContext.current

    LoginButton(
        provider = provider,
        onLaunchLoginFlow = { launcher, params ->
            firstRunModel.launchLoginFlow(context, params, launcher)
        },
        onLaunchActivityResult = { result, params ->
            firstRunModel.handleLoginActivityResult(context, result, params) {
                appNavModel.openUrlInNewTab(it)
            }
        },
    )
}

@Composable
private fun LoginButton(
    provider: NeevaUser.SSOProvider,
    onLaunchLoginFlow: (ActivityResultLauncher<Intent>, LaunchLoginFlowParams) -> Unit,
    onLaunchActivityResult: (ActivityResult, LaunchLoginFlowParams) -> Unit,
) {
    val launchLoginFlowParams = LaunchLoginFlowParams(
        provider = provider,
        signup = true,
    )

    val resultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult ->
        onLaunchActivityResult(result, launchLoginFlowParams)
    }

    if (provider == NeevaUser.SSOProvider.GOOGLE) {
        LoginButton(
            text = getSSOProviderOnboardingText(provider, true),
            startComposable = { SSOProviderButtonIcon(ssoProvider = provider) },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            onClick = { onLaunchLoginFlow(resultLauncher, launchLoginFlowParams) }
        )
    } else {
        LoginButton(
            text = getSSOProviderOnboardingText(provider, true),
            startComposable = { SSOProviderButtonIcon(ssoProvider = provider) },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            onClick = { onLaunchLoginFlow(resultLauncher, launchLoginFlowParams) }
        )
    }
}

@Composable
fun getSSOProviderOnboardingText(provider: NeevaUser.SSOProvider, signup: Boolean): String {
    return when (provider) {
        NeevaUser.SSOProvider.MICROSOFT ->
            if (signup) {
                stringResource(R.string.sign_up_with_microsoft)
            } else {
                stringResource(R.string.sign_in_with_microsoft)
            }

        NeevaUser.SSOProvider.GOOGLE ->
            if (signup) {
                stringResource(R.string.sign_up_with_google)
            } else {
                stringResource(R.string.sign_in_with_google)
            }

        NeevaUser.SSOProvider.OKTA -> stringResource(R.string.sign_up_with_email)

        else -> throw IllegalStateException("Unsupported SSO Provider!")
    }
}

/** Standardized button used for the Welcome Flow. */
@Composable
fun LoginButton(
    text: String,
    containerColor: Color,
    contentColor: Color,
    border: BorderStroke? = null,
    startComposable: @Composable (() -> Unit)? = null,
    endComposable: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        border = border,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = Dimensions.PADDING_MEDIUM)
        ) {
            startComposable?.let {
                it()
                Spacer(modifier = Modifier.width(Dimensions.PADDING_SMALL))
            }

            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium
            )

            endComposable?.let {
                Spacer(modifier = Modifier.width(Dimensions.PADDING_SMALL))
                it()
            }
        }
    }
}
