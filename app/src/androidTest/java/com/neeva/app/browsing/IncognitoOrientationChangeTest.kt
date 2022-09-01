// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.PresetSharedPreferencesRule
import com.neeva.app.R
import com.neeva.app.getString
import com.neeva.app.openCardGrid
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForNodeWithText
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class IncognitoOrientationChangeTest : BaseBrowserTest() {
    @get:Rule
    val presetSharedPreferencesRule = PresetSharedPreferencesRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @Test
    fun doesNotCrashOnOrientationChange() {
        // Checks for regressions against https://github.com/neevaco/neeva-android/issues/452
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()

            // Open the incognito tab switcher.
            openCardGrid(incognito = true)

            // Confirm that we're looking at an empty incognito TabGrid.
            waitForNodeWithText(getString(R.string.tab_switcher_no_incognito_tabs)).assertExists()

            // Rotate the screen, which normally triggers the crash.
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            waitForIdle()
        }
    }
}
