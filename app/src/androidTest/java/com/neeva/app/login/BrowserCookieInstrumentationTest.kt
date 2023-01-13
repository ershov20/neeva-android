// Copyright 2023 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.login

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.neeva.app.BaseHiltTest
import com.neeva.app.NeevaActivity
import com.neeva.app.PresetSharedPreferencesRule
import com.neeva.app.userdata.LoginToken
import com.neeva.app.waitFor
import com.neeva.app.waitForActivityStartup
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@HiltAndroidTest
class BrowserCookieInstrumentationTest : BaseHiltTest() {
    @Inject lateinit var loginToken: LoginToken

    @get:Rule
    val presetSharedPreferencesRule =
        PresetSharedPreferencesRule(skipFirstRun = false, skipNeevaScopeTooltip = true)

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @Test
    fun updateBrowserCookieJar_whenBrowserCreatedAndTokenNotEmpty_cookieExistsInBrowserCookieJar() {
        loginToken.updateCachedCookie("myToken")

        androidComposeRule.apply {
            expectThat(loginToken.cachedValue).isEqualTo("myToken")
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()

            var browserCookieValue: String? = "unset"

            activity.lifecycleScope.launch(activity.dispatchers.io) {
                browserCookieValue = loginToken.getOrFetchCookie()
            }

            waitFor {
                browserCookieValue != "unset"
            }

            expectThat(loginToken.cachedValue).isEqualTo(browserCookieValue)
        }
    }
}
