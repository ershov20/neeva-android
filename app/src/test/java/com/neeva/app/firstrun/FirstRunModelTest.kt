// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.firstrun

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResult
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.tasks.Tasks
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.NeevaConstants
import com.neeva.app.logging.ClientLogger
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.ui.PopupModel
import com.neeva.app.userdata.LoginToken
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.userdata.PreviewSessionToken
import com.neeva.testcommon.apollo.TestAuthenticatedApolloWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class FirstRunModelTest : BaseTest() {
    @Rule
    @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    @Mock private lateinit var oktaSignUpHandler: OktaSignUpHandler
    @Mock private lateinit var previewSessionToken: PreviewSessionToken

    private lateinit var context: Context
    private lateinit var firstRunModel: FirstRunModel
    private lateinit var apolloWrapper: TestAuthenticatedApolloWrapper
    private lateinit var settingsDataModel: SettingsDataModel

    private var signInAccount: GoogleSignInAccount? = null

    override fun setUp() {
        super.setUp()

        val neevaConstants = NeevaConstants()

        context = ApplicationProvider.getApplicationContext()
        val sharedPreferencesModel = SharedPreferencesModel(context)
        val loginToken = LoginToken(
            coroutineScope = coroutineScopeRule.scope,
            dispatchers = coroutineScopeRule.dispatchers,
            sharedPreferencesModel = sharedPreferencesModel,
            neevaConstants = neevaConstants
        )

        apolloWrapper = TestAuthenticatedApolloWrapper(
            loginToken = loginToken,
            previewSessionToken = previewSessionToken,
            neevaConstants = neevaConstants
        )

        settingsDataModel = SettingsDataModel(sharedPreferencesModel)

        val clientLogger = ClientLogger(
            authenticatedApolloWrapper = apolloWrapper,
            coroutineScope = coroutineScopeRule.scope,
            dispatchers = coroutineScopeRule.dispatchers,
            neevaConstants = neevaConstants,
            loginToken = loginToken,
            sharedPreferencesModel = sharedPreferencesModel,
            settingsDataModel = settingsDataModel
        )
        val popupModel = PopupModel(
            coroutineScopeRule.scope,
            coroutineScopeRule.dispatchers,
            sharedPreferencesModel
        )

        signInAccount = mock {
            on { idToken } doReturn "valid_token"
            on { serverAuthCode } doReturn "valid_code"
        }

        firstRunModel = FirstRunModel(
            clientLogger = clientLogger,
            coroutineScope = coroutineScopeRule.scope,
            dispatchers = coroutineScopeRule.dispatchers,
            googleSignInAccountProvider = { Tasks.forResult(signInAccount) },
            oktaSignUpHandler = oktaSignUpHandler,
            loginToken = loginToken,
            neevaConstants = neevaConstants,
            popupModel = popupModel,
            sharedPreferencesModel = sharedPreferencesModel,
            settingsDataModel = settingsDataModel
        )
    }

    @Test
    fun handleLoginActivityResult_onSuccessCalledWithValidResult() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            // Mock a GoogleSignInAccount that will return valid results for success and set it.
            val intent = Intent()
            val activityResult = mock<ActivityResult> {
                on { resultCode } doReturn Activity.RESULT_OK
                on { data } doReturn intent
            }

            var onSuccessCalled = false
            val onSuccess = { _: Uri -> onSuccessCalled = true }

            firstRunModel.handleLoginActivityResult(
                context,
                activityResult,
                onSuccess = onSuccess,
                launchLoginFlowParams = LaunchLoginFlowParams(
                    provider = NeevaUser.SSOProvider.GOOGLE,
                    signup = false,
                    emailProvided = "unused@gmail.com",
                    passwordProvided = null
                )
            )

            expectThat(onSuccessCalled).isTrue()
        }

    @Test
    fun handleLoginActivityResult_failedResultDoesntCallOnSuccess() =
        runTest(coroutineScopeRule.scope.testScheduler) {
            val activityResult = mock<ActivityResult> {
                on { resultCode } doReturn Activity.RESULT_CANCELED
            }
            signInAccount = null

            var onSuccessCalled = false
            val onSuccess = { _: Uri -> onSuccessCalled = true }

            firstRunModel.handleLoginActivityResult(
                context,
                activityResult,
                onSuccess = onSuccess,
                launchLoginFlowParams = LaunchLoginFlowParams(
                    provider = NeevaUser.SSOProvider.GOOGLE,
                    signup = false,
                    emailProvided = "unused@gmail.com",
                    passwordProvided = null
                )
            )

            expectThat(onSuccessCalled).isFalse()
        }
}
