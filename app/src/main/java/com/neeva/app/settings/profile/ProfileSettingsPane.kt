// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.settings.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.neeva.app.LocalNeevaConstants
import com.neeva.app.NeevaConstants
import com.neeva.app.settings.SettingsController
import com.neeva.app.settings.mockSettingsControllerImpl
import com.neeva.app.settings.sharedcomposables.SettingsPane
import com.neeva.app.type.SubscriptionType
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.PortraitPreviewsDark

@Composable
fun ProfileSettingsPane(
    settingsController: SettingsController,
    neevaConstants: NeevaConstants
) {
    val userInfo by settingsController.getNeevaUserInfoFlow().collectAsState()
    val hasBasicSubscription = userInfo == null ||
        userInfo?.subscriptionType == SubscriptionType.Basic
    SettingsPane(
        settingsController,
        ProfileSettingsPaneData(
            neevaConstants = neevaConstants,
            hasBasicSubscription = hasBasicSubscription
        )
    )
}

@Composable
private fun ProfileSettingsPane(
    settingsController: SettingsController,
    neevaConstants: NeevaConstants,
    hasBasic: Boolean
) {
    SettingsPane(
        settingsController,
        ProfileSettingsPaneData(
            neevaConstants = neevaConstants,
            hasBasicSubscription = hasBasic
        )
    )
}

@PortraitPreviews
@Composable
fun SettingsProfile_BasicSubscription_Preview() {
    NeevaThemePreviewContainer(
        useDarkTheme = false,
        addBorder = false
    ) {
        ProfileSettingsPane(
            settingsController = mockSettingsControllerImpl,
            neevaConstants = LocalNeevaConstants.current,
            hasBasic = true
        )
    }
}

@PortraitPreviewsDark
@Composable
fun SettingsProfile_BasicSubscription_Preview_Dark() {
    NeevaThemePreviewContainer(
        useDarkTheme = true,
        addBorder = false
    ) {
        ProfileSettingsPane(
            settingsController = mockSettingsControllerImpl,
            neevaConstants = LocalNeevaConstants.current,
            hasBasic = true
        )
    }
}

@PortraitPreviews
@Composable
fun SettingsProfile_PremiumSubscription_Preview() {
    NeevaThemePreviewContainer(
        useDarkTheme = false,
        addBorder = false
    ) {
        ProfileSettingsPane(
            settingsController = mockSettingsControllerImpl,
            neevaConstants = LocalNeevaConstants.current,
            hasBasic = false
        )
    }
}

@PortraitPreviewsDark
@Composable
fun SettingsProfile_PremiumSubscription_Preview_Dark() {
    NeevaThemePreviewContainer(
        useDarkTheme = true,
        addBorder = false
    ) {
        ProfileSettingsPane(
            settingsController = mockSettingsControllerImpl,
            neevaConstants = LocalNeevaConstants.current,
            hasBasic = false
        )
    }
}
