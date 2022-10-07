// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.history

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import com.apollographql.apollo3.api.Optional
import com.neeva.app.BaseBrowserTest
import com.neeva.app.LogMutation
import com.neeva.app.NeevaActivity
import com.neeva.app.PresetSharedPreferencesRule
import com.neeva.app.apollo.AuthenticatedApolloWrapper
import com.neeva.app.expectBrowserState
import com.neeva.app.logging.LogConfig
import com.neeva.app.visitSite
import com.neeva.app.waitForActivityStartup
import com.neeva.testcommon.WebpageServingRule
import com.neeva.testcommon.apollo.TestAuthenticatedApolloWrapper
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isTrue

@HiltAndroidTest
class ClientLoggerTest : BaseBrowserTest() {
    private val testUrl = WebpageServingRule.urlFor("big_link_element_target_blank.html")

    @get:Rule
    val presetSharedPreferencesRule = PresetSharedPreferencesRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @Inject
    lateinit var authenticatedApolloWrapper: AuthenticatedApolloWrapper

    private lateinit var testAuthenticatedApolloWrapper: TestAuthenticatedApolloWrapper

    @Before
    override fun setUp() {
        super.setUp()
        testAuthenticatedApolloWrapper =
            authenticatedApolloWrapper as TestAuthenticatedApolloWrapper
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()
            expectBrowserState(isIncognito = false, regularTabCount = 1)
        }
    }

    @Test
    fun pageLoadLogging() {
        androidComposeRule.apply {
            visitSite()

            expectThat(
                containsEvent(
                    testAuthenticatedApolloWrapper.testApolloClientWrapper.performedOperations,
                    LogConfig.Interaction.NAVIGATION_INBOUND
                )
            ).isTrue()
        }
    }

    fun containsEvent(operations: List<Any>, interaction: LogConfig.Interaction): Boolean {
        operations.forEach {
            if (it is LogMutation) {
                val log = it.input.log
                if (log.isNotEmpty()) {
                    val counter = log[0].counter
                    if (counter is Optional.Present &&
                        counter.value?.path ==
                        interaction.interactionName
                    ) {
                        return true
                    }
                }
            }
        }
        return false
    }
}
