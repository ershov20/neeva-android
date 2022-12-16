// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.zeroquery

import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performScrollToNode
import androidx.lifecycle.Lifecycle
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.PresetSharedPreferencesRule
import com.neeva.app.R
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.assertionToBoolean
import com.neeva.app.clickOnUrlBar
import com.neeva.app.expectBrowserState
import com.neeva.app.flakyClickOnNode
import com.neeva.app.getString
import com.neeva.app.waitForActivityStartup
import com.neeva.app.waitForNavDestination
import com.neeva.app.waitForNode
import com.neeva.app.waitForNodeToDisappear
import com.neeva.app.waitForNodeWithTag
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.CoreMatchers.allOf
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class RateNeevaPromoTest : BaseBrowserTest() {
    @get:Rule
    val presetSharedPreferencesRule = PresetSharedPreferencesRule()

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    fun stringMatcher(resourceId: Int) = hasText(
        androidComposeRule.getString(resourceId)
    )

    val doItMatcher by lazy { stringMatcher(R.string.lets_do_it) }
    val experienceMatcher by lazy { stringMatcher(R.string.neeva_experience) }
    val lovingItMatcher by lazy { stringMatcher(R.string.loving_it) }
    val maybeLaterMatcher by lazy { stringMatcher(R.string.maybe_later) }
    val needsWorkMatcher by lazy { stringMatcher(R.string.needs_work) }
    val provideFeedbackMatcher by lazy { stringMatcher(R.string.send_feedback) }
    val rateCTAMatcher by lazy { stringMatcher(R.string.rate_on_play_store) }

    override fun setUp() {
        super.setUp()
        Intents.init()
        androidComposeRule.apply {
            activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            waitForActivityStartup()
            expectBrowserState(isIncognito = false, regularTabCount = 1)
        }
    }

    fun waitForZeroQuery(content: SemanticsNodeInteraction.() -> Unit) {
        androidComposeRule.apply {
            clickOnUrlBar()
            waitForNodeWithTag("RegularProfileZeroQuery").apply(content)
        }
    }

    @Test
    fun itAppearsWhenZeroQueryIsVisible() {
        waitForZeroQuery {
            assertionToBoolean {
                performScrollToNode(experienceMatcher).assertIsDisplayed()
            }
        }
    }

    fun getToRateState() {
        androidComposeRule.apply {
            waitForZeroQuery {
                performScrollToNode(lovingItMatcher)
                flakyClickOnNode(
                    lovingItMatcher
                ) {
                    assertionToBoolean {
                        androidComposeRule.waitForNode(rateCTAMatcher).assertIsDisplayed()
                    }
                }
            }
        }
    }

    @Test
    fun tappingOnLovingItAsksUserToRate() {
        getToRateState()
    }

    fun getToFeedbackState() {
        androidComposeRule.apply {
            waitForZeroQuery {
                performScrollToNode(needsWorkMatcher)
                flakyClickOnNode(needsWorkMatcher) {
                    assertionToBoolean {
                        androidComposeRule.waitForNode(provideFeedbackMatcher).assertIsDisplayed()
                    }
                }
            }
        }
    }

    @Test
    fun tappingOnNeedsWorkAsksUserToProvideFeedback() {
        getToFeedbackState()
    }

    @Test
    fun feedbackState_isNotVisibleAfterTappingMaybeLater() {
        androidComposeRule.apply {
            getToFeedbackState()
            flakyClickOnNode(maybeLaterMatcher) {
                assertionToBoolean {
                    waitForNodeToDisappear(onNode(maybeLaterMatcher))
                    waitForNodeToDisappear(onNode(provideFeedbackMatcher))
                    waitForNodeToDisappear(onNode(experienceMatcher))
                }
            }
        }
    }

    @Test
    fun rateState_isNotVisibleAfterTappingMaybeLater() {
        androidComposeRule.apply {
            getToRateState()
            flakyClickOnNode(maybeLaterMatcher) {
                assertionToBoolean {
                    waitForNodeToDisappear(onNode(maybeLaterMatcher))
                    waitForNodeToDisappear(onNode(rateCTAMatcher))
                    waitForNodeToDisappear(onNode(experienceMatcher))
                }
            }
        }
    }

    @Test
    fun feedbackState_tappingYesNavigatesToSupportPage() {
        androidComposeRule.apply {
            getToFeedbackState()
            flakyClickOnNode(doItMatcher) {
                assertionToBoolean {
                    waitForNavDestination(AppNavDestination.FEEDBACK)
                }
            }
        }
    }

    @Test
    fun rateState_tappingRateLaunchesPlayStoreViaIntent() {
        androidComposeRule.apply {
            getToRateState()
            flakyClickOnNode(doItMatcher) {
                assertionToBoolean {
                    intended(
                        allOf(
                            hasAction(Intent.ACTION_VIEW),
                            hasData(Uri.parse("market://details?id=com.neeva.app"))
                        )
                    )
                }
            }
        }
    }
}
