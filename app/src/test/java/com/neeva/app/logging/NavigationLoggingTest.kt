// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.logging

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.apollographql.apollo3.api.Optional
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.LogMutation
import com.neeva.app.NeevaConstants
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.userdata.LoginToken
import com.neeva.app.userdata.PreviewSessionToken
import com.neeva.testcommon.apollo.TestAuthenticatedApolloWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.isTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class NavigationLoggingTest : BaseTest() {
    @Rule
    @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    @Mock private lateinit var previewSessionToken: PreviewSessionToken

    private lateinit var context: Context
    private lateinit var apolloWrapper: TestAuthenticatedApolloWrapper
    private lateinit var settingsDataModel: SettingsDataModel
    private lateinit var clientLogger: ClientLogger
    private lateinit var sharedPreferencesModel: SharedPreferencesModel

    override fun setUp() {
        super.setUp()

        val neevaConstants = NeevaConstants()

        context = ApplicationProvider.getApplicationContext()
        sharedPreferencesModel = SharedPreferencesModel(context)
        val loginToken = LoginToken(
            coroutineScope = coroutineScopeRule.scope,
            dispatchers = coroutineScopeRule.dispatchers,
            sharedPreferencesModel = sharedPreferencesModel,
            neevaConstants = neevaConstants
        )

        SharedPrefFolder.FirstRun.FirstRunDone.set(
            sharedPreferencesModel = sharedPreferencesModel,
            value = true,
            mustCommitImmediately = true
        )

        apolloWrapper = TestAuthenticatedApolloWrapper(
            loginToken = loginToken,
            previewSessionToken = previewSessionToken,
            neevaConstants = neevaConstants
        )

        settingsDataModel = SettingsDataModel(sharedPreferencesModel)
        settingsDataModel.setToggleState(SettingsToggle.LOGGING_CONSENT, true)

        clientLogger = ClientLogger(
            authenticatedApolloWrapper = apolloWrapper,
            coroutineScope = coroutineScopeRule.scope,
            dispatchers = coroutineScopeRule.dispatchers,
            neevaConstants = neevaConstants,
            loginToken = loginToken,
            sharedPreferencesModel = sharedPreferencesModel,
            settingsDataModel = settingsDataModel
        )
    }

    @Test
    fun handleLoginActivityResult_outboundNavigation() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            clientLogger.logNavigation(Uri.parse("https://www.outbound.com"))

            coroutineScopeRule.scope.advanceUntilIdle()

            expectThat(
                containsEvent(
                    apolloWrapper.testApolloClientWrapper.performedOperations,
                    LogConfig.Interaction.NAVIGATION_OUTBOUND
                )
            ).isTrue()
        }

    @Test
    fun handleLoginActivityResult_inboundNavigation() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            clientLogger.logNavigation(Uri.parse("https://neeva.com/inbound"))

            coroutineScopeRule.scope.advanceUntilIdle()

            expectThat(
                containsEvent(
                    apolloWrapper.testApolloClientWrapper.performedOperations,
                    LogConfig.Interaction.NAVIGATION_INBOUND
                )
            ).isTrue()
        }

    @Test
    fun handleLoginActivityResult_searchWithLoginTokenNavigation() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            SharedPrefFolder.User.Token.set(
                sharedPreferencesModel = sharedPreferencesModel,
                value = "cachedToken",
                mustCommitImmediately = true
            )
            clientLogger.logNavigation(
                Uri.parse("https://neeva.com/search?q=test&c=All&src=typedquery")
            )

            coroutineScopeRule.scope.advanceUntilIdle()

            // For logged in user, we don't send any logging when they perform a search
            expectThat(apolloWrapper.testApolloClientWrapper.performedOperations.isEmpty())
        }

    @Test
    fun handleLoginActivityResult_searchNoLoginTokenNavigation() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            clientLogger.logNavigation(
                Uri.parse("https://neeva.com/search?q=test&c=All&src=typedquery")
            )

            coroutineScopeRule.scope.advanceUntilIdle()

            expectThat(
                containsEvent(
                    apolloWrapper.testApolloClientWrapper.performedOperations,
                    LogConfig.Interaction.PREVIEW_SEARCH
                )
            ).isTrue()
        }

    fun containsEvent(operations: List<Any>, interaction: LogConfig.Interaction): Boolean {
        operations.forEach {
            if (it is LogMutation) {
                val log = it.input.log
                if (log.isEmpty()) return@forEach

                val counter = log[0].counter
                if (counter is Optional.Present &&
                    counter.value?.path == interaction.interactionName
                ) {
                    return true
                }
            }
        }
        return false
    }
}
