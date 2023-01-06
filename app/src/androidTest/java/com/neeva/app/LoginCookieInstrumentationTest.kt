// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.neeva.app.apollo.AuthenticatedApolloWrapper
import com.neeva.app.type.SubscriptionType
import com.neeva.app.userdata.LoginToken
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.userdata.NeevaUserImpl.Companion.toUserInfo
import com.neeva.app.userdata.UserInfo
import com.neeva.testcommon.WebpageServingRule
import com.neeva.testcommon.apollo.TestAuthenticatedApolloWrapper
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

@HiltAndroidTest
class LoginCookieInstrumentationTest : BaseHiltTest() {
    private val loggedInUserInfo: UserInfo = UserInfo(
        "my-id",
        "some display name",
        "email@neeva.co",
        "https://www.cdn/my-image.png",
        NeevaUser.SSOProvider.GOOGLE.name
    )

    @Inject lateinit var authenticatedApolloWrapper: AuthenticatedApolloWrapper
    @Inject lateinit var loginToken: LoginToken
    @Inject lateinit var neevaConstants: NeevaConstants
    @Inject lateinit var neevaUser: NeevaUser

    private lateinit var testApolloWrapper: TestAuthenticatedApolloWrapper

    @get:Rule
    val webpageServingRule = WebpageServingRule()

    @get:Rule
    val presetSharedPreferencesRule =
        PresetSharedPreferencesRule(skipFirstRun = false, skipNeevaScopeTooltip = true)

    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    private fun setUpLoggedInUser() {
        loginToken.updateCachedCookie("myToken")
        neevaUser.setUserInfo(loggedInUserInfo)
    }

    override fun setUp() {
        super.setUp()
        testApolloWrapper = authenticatedApolloWrapper as TestAuthenticatedApolloWrapper
        setUpLoggedInUser()
    }

    private fun waitForUserInfoQuery() {
        androidComposeRule.apply {
            waitFor {
                testApolloWrapper.testApolloClientWrapper.performedOperations.any {
                    (it as? UserInfoQuery) != null
                }
            }
        }
    }

    @Test
    fun sessionToken_whenNotSignedIn_clearsCachedData() {
        androidComposeRule.apply {
            startActivity()

            // We haven't set up a response, so the UserInfo query will fail.  We should keep the
            // old data anyway.
            waitForUserInfoQuery()
            expectThat(loginToken.cachedValue).isEqualTo("myToken")
            expectThat(activity.neevaUser.userInfoFlow.value).isEqualTo(loggedInUserInfo)

            // Clear out the cookie.
            loginToken.updateCachedCookie("")

            expectThat(loginToken.cachedValue).isEmpty()
            expectThat(activity.neevaUser.userInfoFlow.value).isNull()

            activityRule.scenario.close()
        }
    }

    @Test
    fun sessionToken_whenBrowserTokenUpdated_updatesCachedTokenAndFetchesUserInfo() {
        androidComposeRule.apply {
            startActivity()

            // We haven't set up a response, so the UserInfo query will fail.  We should keep the
            // old data anyway.
            waitForUserInfoQuery()
            expectThat(loginToken.cachedValue).isEqualTo("myToken")
            expectThat(activity.neevaUser.userInfoFlow.value).isEqualTo(loggedInUserInfo)

            // Reset the GraphQL operations that have been performed.
            testApolloWrapper.testApolloClientWrapper.performedOperations.clear()

            // Say that a fetch of the UserInfo will return new data.
            val newUserInfoResponse = UserInfoQuery.Data(
                user = UserInfoQuery.User(
                    id = "new-id",
                    profile = UserInfoQuery.Profile(
                        displayName = "updated display name",
                        email = "email@neeva.co",
                        pictureURL = "http://127.0.0.1:8000/favicon.ico"
                    ),
                    flags = emptyList(),
                    featureFlags = emptyList(),
                    authProvider = NeevaUser.SSOProvider.GOOGLE.name,
                    subscription = null,
                    subscriptionType = SubscriptionType.Basic
                )
            )
            testApolloWrapper.registerTestResponse(
                operation = UserInfoQuery(),
                response = newUserInfoResponse
            )

            // Update the token in the Browser.  Flows should update the cached value of the cookie.
            activity.runOnUiThread {
                loginToken.updateCookieManager(newValue = "newToken")
            }
            waitFor { loginToken.cachedValue == "newToken" }

            // The cached value getting updated should result in an attempt to refetch UserInfo.
            waitForUserInfoQuery()
            val expectedNewUserInfo = newUserInfoResponse.user!!.toUserInfo()
            waitFor {
                neevaUser.userInfoFlow.value == expectedNewUserInfo
            }

            // Reset the ApolloWrapper's state so that trying to perform a UserInfoQuery will fail.
            testApolloWrapper.registerTestResponse(
                operation = UserInfoQuery(),
                response = null
            )
            testApolloWrapper.testApolloClientWrapper.performedOperations.clear()

            // Update the cookie to trigger the flows.
            activity.runOnUiThread {
                loginToken.updateCookieManager(newValue = "evenNewerToken")
            }
            waitFor { loginToken.cachedValue == "evenNewerToken" }

            // The cached value getting updated should result in an attempt to refetch UserInfo,
            // which will fail because we didn't mock out the response.
            waitForUserInfoQuery()
            waitFor {
                neevaUser.userInfoFlow.value == expectedNewUserInfo
            }
        }
    }

    @Test
    fun signOut_whenLoggedIn_clearsUserAndNeevaUserTokenAndCookies() {
        androidComposeRule.apply {
            startActivity()

            // We haven't set up a response, so the UserInfo query will fail.  We should keep the
            // old data anyway.
            waitForUserInfoQuery()
            expectThat(loginToken.cachedValue).isEqualTo("myToken")
            expectThat(activity.neevaUser.userInfoFlow.value).isEqualTo(loggedInUserInfo)

            // Reset the GraphQL operations that have been performed.
            testApolloWrapper.testApolloClientWrapper.performedOperations.clear()

            // Say that a fetch of the UserInfo will return new data.
            val newUserInfoResponse = UserInfoQuery.Data(
                user = UserInfoQuery.User(
                    id = "new-id",
                    profile = UserInfoQuery.Profile(
                        displayName = "updated display name",
                        email = "email@neeva.co",
                        pictureURL = "http://127.0.0.1:8000/favicon.ico"
                    ),
                    flags = emptyList(),
                    featureFlags = emptyList(),
                    authProvider = NeevaUser.SSOProvider.GOOGLE.name,
                    subscription = null,
                    subscriptionType = SubscriptionType.Basic
                )
            )
            testApolloWrapper.registerTestResponse(
                operation = UserInfoQuery(),
                response = newUserInfoResponse
            )

            // Update the token in the Browser.  Flows should update the cached value of the cookie.
            activity.runOnUiThread {
                loginToken.updateCookieManager(newValue = "newToken")
            }
            waitFor { loginToken.cachedValue == "newToken" }

            // The cached value getting updated should result in an attempt to refetch UserInfo.
            waitForUserInfoQuery()
            val expectedNewUserInfo = newUserInfoResponse.user!!.toUserInfo()
            waitFor {
                neevaUser.userInfoFlow.value == expectedNewUserInfo
            }

            // Signing out should clear everything.
            var success: Boolean? = null
            runOnUiThread {
                activity.activityViewModel.signOut {
                    success = it
                }
            }

            waitFor { neevaUser.loginToken.cachedValue.isEmpty() }
            waitFor { neevaUser.userInfoFlow.value == null }
            waitFor { success == true }
        }
    }
}
