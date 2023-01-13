// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.welcomeflow

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalFirstRunModel
import com.neeva.app.LocalSettingsDataModel
import com.neeva.app.R
import com.neeva.app.firstrun.LegalFooter
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions

@Composable
fun WelcomeScreen(onContinueInWelcomeScreen: () -> Unit, navigateToSignIn: () -> Unit) {
    val settingsDataModel = LocalSettingsDataModel.current
    WelcomeFlowContainer(headerText = stringResource(id = R.string.welcomeflow_initial_header)) {
        WelcomeScreenContent(
            onContinueInWelcomeScreen = onContinueInWelcomeScreen,
            navigateToSignIn = navigateToSignIn,
            loggingConsentState = settingsDataModel.getToggleState(SettingsToggle.LOGGING_CONSENT),
            toggleLoggingConsentState = settingsDataModel
                .getTogglePreferenceToggler(SettingsToggle.LOGGING_CONSENT),
            modifier = it
        )
    }
}

@Composable
fun WelcomeScreenContent(
    onContinueInWelcomeScreen: () -> Unit,
    navigateToSignIn: () -> Unit,
    loggingConsentState: MutableState<Boolean>,
    toggleLoggingConsentState: () -> Unit,
    modifier: Modifier
) {
    val firstRunModel = LocalFirstRunModel.current
    val context = LocalContext.current

    Column(modifier = modifier) {
        Spacer(Modifier.height(80.dp))

        MainBenefit(
            title = stringResource(id = R.string.welcomeflow_privacy_benefit),
            description = stringResource(id = R.string.welcomeflow_privacy_benefit_description)
        )

        Spacer(Modifier.height(18.dp))

        MainBenefit(
            title = stringResource(id = R.string.welcomeflow_unbiased_benefit),
            description = stringResource(id = R.string.welcomeflow_unbiased_benefit_description)
        )

        Spacer(Modifier.height(48.dp))

        ContinueButtons(
            onContinueInWelcomeScreen = onContinueInWelcomeScreen,
            navigateToSignIn = navigateToSignIn
        )

        ConsentCheckbox(
            loggingConsentState = loggingConsentState,
            toggleLoggingConsentState = toggleLoggingConsentState
        )

        LegalFooter(
            modifier = Modifier
                .defaultMinSize(minHeight = dimensionResource(R.dimen.min_touch_target_size))
                .fillMaxWidth(),
            onOpenUrl = { uri -> firstRunModel.openSingleTabActivity(context, uri) }
        )

        Spacer(Modifier.height(Dimensions.PADDING_HUGE))
    }
}

@Composable
internal fun ConsentCheckbox(
    loggingConsentState: MutableState<Boolean>,
    modifier: Modifier = Modifier,
    toggleLoggingConsentState: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable { toggleLoggingConsentState() }
    ) {
        Checkbox(
            checked = loggingConsentState.value,
            onCheckedChange = null,
            modifier = Modifier.size(Dimensions.SIZE_TOUCH_TARGET)
        )

        Text(
            text = stringResource(R.string.logging_consent),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ContinueButtons(
    onContinueInWelcomeScreen: () -> Unit,
    navigateToSignIn: () -> Unit,
) {
    WelcomeFlowStackedButtons(
        primaryText = stringResource(id = R.string.welcomeflow_lets_go),
        onPrimaryButton = onContinueInWelcomeScreen,
        secondaryText = stringResource(id = R.string.welcomeflow_i_have_an_account),
        onSecondaryButton = navigateToSignIn
    )
}

@PortraitPreviews
@Composable
fun WelcomeScreen_Light_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        WelcomeScreen(
            navigateToSignIn = {},
            onContinueInWelcomeScreen = {},
        )
    }
}

@PortraitPreviews
@Composable
fun WelcomeScreen_Dark_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        WelcomeScreen(
            navigateToSignIn = {},
            onContinueInWelcomeScreen = {}
        )
    }
}
