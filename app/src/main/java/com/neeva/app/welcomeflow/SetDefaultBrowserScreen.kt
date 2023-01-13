// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.welcomeflow

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalClientLogger
import com.neeva.app.R
import com.neeva.app.logging.LogConfig
import com.neeva.app.settings.defaultbrowser.SetDefaultAndroidBrowserManager
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.PortraitPreviewsDark
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun SetDefaultBrowserScreen(
    openAndroidDefaultBrowserSettings: () -> Unit,
    setDefaultAndroidBrowserManager: SetDefaultAndroidBrowserManager,
    finishWelcomeFlow: () -> Unit
) {
    val clientLogger = LocalClientLogger.current
    val isRoleManagerAvailable = setDefaultAndroidBrowserManager.isRoleManagerAvailable()

    SetDefaultBrowserScreen(
        isRoleManagerAvailable = isRoleManagerAvailable,
        onSetDefaultBrowser = {
            clientLogger.logCounter(
                LogConfig.Interaction.DEFAULT_BROWSER_ONBOARDING_INTERSTITIAL_OPEN,
                null
            )
            if (isRoleManagerAvailable) {
                setDefaultAndroidBrowserManager.requestToBeDefaultBrowser { neevaIsDefault ->
                    if (neevaIsDefault) {
                        finishWelcomeFlow()
                    }
                }
            } else {
                openAndroidDefaultBrowserSettings()
            }
        },
        onRemindMeLater = {
            clientLogger.logCounter(
                LogConfig.Interaction.DEFAULT_BROWSER_ONBOARDING_INTERSTITIAL_REMIND,
                null
            )
            finishWelcomeFlow()
        }
    )
}

@Composable
private fun SetDefaultBrowserScreen(
    isRoleManagerAvailable: Boolean,
    onSetDefaultBrowser: () -> Unit,
    onRemindMeLater: () -> Unit,
) {
    WelcomeFlowContainer(headerText = stringResource(id = R.string.switch_default_browser_title)) {
        if (isRoleManagerAvailable) {
            SetDefaultBrowserContent(
                onSetDefaultBrowser = onSetDefaultBrowser,
                onRemindMeLater = onRemindMeLater,
                modifier = it
            )
        } else {
            OldOSContent(
                onSetDefaultBrowser = onSetDefaultBrowser,
                onRemindMeLater = onRemindMeLater,
                modifier = it
            )
        }
    }
}

@Composable
private fun SetDefaultBrowserContent(
    onSetDefaultBrowser: () -> Unit,
    onRemindMeLater: () -> Unit,
    modifier: Modifier
) {
    Column(modifier = modifier) {
        Spacer(Modifier.height(80.dp))
        MainBenefit(
            title = stringResource(id = R.string.default_browser_protected_benefit),
            description = stringResource(
                id = R.string.default_browser_protected_benefit_description
            )
        )
        Spacer(Modifier.height(Dimensions.PADDING_LARGE))
        MainBenefit(
            title = stringResource(id = R.string.default_browser_faster_benefit),
            description = stringResource(
                id = R.string.default_browser_faster_benefit_description
            )
        )
        Spacer(Modifier.height(20.dp))
        ContentFilterPromo()
        Spacer(Modifier.height(Dimensions.PADDING_MEDIUM))
        WelcomeFlowStackedButtons(
            primaryText = stringResource(id = R.string.default_browser_set_button_text),
            onPrimaryButton = onSetDefaultBrowser,
            secondaryText = stringResource(id = R.string.default_browser_remind_me_later),
            onSecondaryButton = onRemindMeLater
        )
    }
}

@Composable
private fun ContentFilterPromo() {
    Surface(
        shape = RoundedCornerShape(Dimensions.RADIUS_SMALL),
        contentColor = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row {
            Spacer(Modifier.width(Dimensions.PADDING_HUGE))
            Image(
                painter = painterResource(R.drawable.welcome_content_filter_promo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(Dimensions.PADDING_HUGE))
        }
    }
}

@Composable
private fun OldOSContent(
    onSetDefaultBrowser: () -> Unit,
    onRemindMeLater: () -> Unit,
    modifier: Modifier
) {
    Column(modifier = modifier) {
        Spacer(Modifier.height(36.dp))
        Text(
            text = stringResource(id = R.string.default_browser_old_os_description),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Dimensions.PADDING_HUGE))
        ContentFilterPromo()
        Spacer(Modifier.height(Dimensions.PADDING_HUGE))
        Text(
            text = stringResource(id = R.string.switch_default_browser_follow_3_steps),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(Dimensions.PADDING_SMALL))
        Text(
            text = stringResource(id = R.string.switch_default_browser_instructions_1),
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(Dimensions.PADDING_SMALL))
        Text(
            text = stringResource(id = R.string.switch_default_browser_instructions_2),
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(Dimensions.PADDING_SMALL))
        Text(
            text = stringResource(id = R.string.switch_default_browser_instructions_3),
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(Dimensions.PADDING_SMALL))
        WelcomeFlowStackedButtons(
            primaryText = stringResource(id = R.string.go_to_settings),
            onPrimaryButton = onSetDefaultBrowser,
            secondaryText = stringResource(id = R.string.default_browser_remind_me_later),
            onSecondaryButton = onRemindMeLater
        )
    }
}

@PortraitPreviews
@Composable
fun SetDefaultBrowserScreen_Light_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        SetDefaultBrowserScreen(
            isRoleManagerAvailable = true,
            onSetDefaultBrowser = {},
            onRemindMeLater = {}
        )
    }
}

@PortraitPreviewsDark
@Composable
fun SetDefaultBrowserScreen_Dark_Preview() {
    NeevaTheme(useDarkTheme = true) {
        SetDefaultBrowserScreen(
            isRoleManagerAvailable = true,
            onSetDefaultBrowser = {},
            onRemindMeLater = {}
        )
    }
}

@PortraitPreviews
@Composable
fun SetDefaultBrowserScreen_OldOS_Light_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        SetDefaultBrowserScreen(
            isRoleManagerAvailable = false,
            onSetDefaultBrowser = {},
            onRemindMeLater = {}
        )
    }
}

@PortraitPreviewsDark
@Composable
fun SetDefaultBrowserScreen_OldOS_Dark_Preview() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        SetDefaultBrowserScreen(
            isRoleManagerAvailable = false,
            onSetDefaultBrowser = {},
            onRemindMeLater = {}
        )
    }
}
