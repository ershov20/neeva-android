// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.userdata.LoginToken
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.userdata.NeevaUserImpl
import com.neeva.app.userdata.UserInfo
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNullOrEmpty

@HiltAndroidTest
class LoginCookieInstrumentationTest : BaseHiltTest() {
    @Inject
    lateinit var neevaConstants: NeevaConstants

    private lateinit var loginToken: LoginToken
    private lateinit var loggedInUserInfo: UserInfo
    private lateinit var neevaUser: NeevaUser
    private lateinit var sharedPreferencesModel: SharedPreferencesModel

    @Inject lateinit var coroutineScope: CoroutineScope
    @Inject lateinit var dispatchers: Dispatchers

    @get:Rule
    val presetSharedPreferencesRule =
        PresetSharedPreferencesRule(skipFirstRun = false, skipNeevaScopeTooltip = true)

    private fun setUpLoggedInUser(context: Context) {
        neevaConstants = neevaConstants
        sharedPreferencesModel = SharedPreferencesModel(context)
        loginToken = LoginToken(
            coroutineScope = coroutineScope,
            dispatchers = dispatchers,
            neevaConstants = neevaConstants,
            sharedPreferencesModel = sharedPreferencesModel
        )

        loginToken.updateCachedCookie("myToken")
        loggedInUserInfo = UserInfo(
            "my-id",
            "some display name",
            "email@neeva.co",
            "https://www.cdn/my-image.png",
            NeevaUser.SSOProvider.GOOGLE.name
        )
        neevaUser = NeevaUserImpl(sharedPreferencesModel, loginToken)
        neevaUser.setUserInfo(loggedInUserInfo)
    }

    private fun onActivityStartedTest(context: Context, test: (activity: NeevaActivity) -> Unit) {
        val intent = Intent.makeMainActivity(ComponentName(context, NeevaActivity::class.java))
        ActivityScenario.launch<NeevaActivity>(intent).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity { activity: NeevaActivity ->
                test(activity)
            }
            scenario.close()
        }
    }

    @Test
    fun signedIn_whenLoggedIn_UserDataCachedAndNeevaUserTokenAndLoginCookieExists() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        setUpLoggedInUser(context)

        onActivityStartedTest(context) { activity ->
            // because NeevaUser.fetch() is run on a fake token ("myToken")
            // ApolloWrapper should give a null response. This should not clear the user data.
            // The token + cookies should still be set to "myToken".
            activity.webLayerModel.currentBrowser
                .getCookiePairs(Uri.parse(neevaConstants.appURL)) {
                    expectThat(
                        it.find { cookiePair -> cookiePair.key == neevaConstants.loginCookie }
                            ?.value
                    ).isEqualTo("myToken")
                }
            expectThat(activity.neevaUser.loginToken.cookieValue).isEqualTo("myToken")
            expectThat(activity.neevaUser.userInfoFlow.value).isEqualTo(loggedInUserInfo)
        }
    }

    @Test
    fun signOut_whenLoggedIn_clearsUserAndNeevaUserTokenAndCookies() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        setUpLoggedInUser(context)

        onActivityStartedTest(context) { activity ->
            activity.activityViewModel.signOut()

            // ^^ sign out should have cleared NeevaUser.data and NeevaUserToken!
            expectThat(activity.neevaUser.loginToken.cookieValue).isEmpty()
            expectThat(activity.neevaUser.userInfoFlow.value).isEqualTo(null)

            activity.webLayerModel.currentBrowser
                .getCookiePairs(Uri.parse(neevaConstants.appURL)) {
                    expectThat(
                        it.find {
                            cookiePair ->
                            cookiePair.key == neevaConstants.loginCookie
                        }?.value
                    ).isNullOrEmpty()
                }
        }
    }
}
