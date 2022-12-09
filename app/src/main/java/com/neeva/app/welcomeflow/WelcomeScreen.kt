// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.welcomeflow

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.neeva.app.LocalSettingsDataModel
import com.neeva.app.R
import com.neeva.app.firstrun.LegalFooter
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.PreviewCompositionLocals
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun WelcomeScreen(navigateToSignUp: () -> Unit) {
    val settingsDataModel = LocalSettingsDataModel.current
    WelcomeFlowContainer(headerText = stringResource(id = R.string.welcomeflow_initial_header)) {
        WelcomeScreenContent(
            navigateToSignUp = navigateToSignUp,
            loggingConsentState = settingsDataModel.getToggleState(SettingsToggle.LOGGING_CONSENT),
            toggleLoggingConsentState = settingsDataModel
                .getTogglePreferenceToggler(SettingsToggle.LOGGING_CONSENT),
            onOpenUrl = {}
        )
    }
}

@Composable
fun WelcomeScreenContent(
    navigateToSignUp: () -> Unit,
    loggingConsentState: MutableState<Boolean>,
    toggleLoggingConsentState: () -> Unit,
    onOpenUrl: (Uri) -> Unit
) {
    Column(Modifier.padding(horizontal = dimensionResource(id = R.dimen.welcome_flow_padding))) {
        Spacer(Modifier.height(80.dp))

        BenefitsRow(
            title = stringResource(id = R.string.welcomeflow_privacy_benefit),
            description = stringResource(id = R.string.welcomeflow_privacy_benefit_description)
        )

        Spacer(Modifier.height(18.dp))

        BenefitsRow(
            title = stringResource(id = R.string.welcomeflow_unbiased_benefit),
            description = stringResource(id = R.string.welcomeflow_unbiased_benefit_description)
        )

        Spacer(Modifier.height(64.dp))

        Button(onClick = navigateToSignUp, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(id = R.string.welcomeflow_lets_go),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(Dimensions.PADDING_MEDIUM)
            )
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = navigateToSignUp,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.welcomeflow_i_have_an_account),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(Dimensions.PADDING_MEDIUM)
            )
        }

        Spacer(Modifier.height(Dimensions.PADDING_LARGE))

        ConsentCheckbox(
            loggingConsentState = loggingConsentState,
            toggleLoggingConsentState = toggleLoggingConsentState
        )

        LegalFooter(
            modifier = Modifier
                .defaultMinSize(minHeight = dimensionResource(R.dimen.min_touch_target_size))
                .fillMaxWidth(),
            onOpenUrl = onOpenUrl
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
fun BenefitsRow(title: String, description: String) {
    ConstraintLayout(Modifier.fillMaxWidth()) {
        val (check, titleRef, descriptionRef) = createRefs()
        Icon(
            painter = painterResource(R.drawable.ic_check_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.constrainAs(check) {
                start.linkTo(parent.start)
                top.linkTo(parent.top)
            }
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.constrainAs(titleRef) {
                top.linkTo(check.top)
                bottom.linkTo(check.bottom)
                start.linkTo(check.end, margin = Dimensions.PADDING_LARGE)
            }
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.constrainAs(descriptionRef) {
                start.linkTo(titleRef.start)
                top.linkTo(titleRef.bottom)
            }
        )
    }
}

@PortraitPreviews
@Composable
fun SignInScreen_Light_Preview() {
    PreviewCompositionLocals {
        NeevaTheme {
            WelcomeScreen(navigateToSignUp = {})
        }
    }
}

@PortraitPreviews
@Composable
fun SignInScreen_Dark_Preview() {
    PreviewCompositionLocals {
        NeevaTheme(useDarkTheme = true) {
            WelcomeScreen(navigateToSignUp = {})
        }
    }
}
