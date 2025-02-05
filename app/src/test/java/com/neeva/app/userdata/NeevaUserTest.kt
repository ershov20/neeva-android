// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.userdata

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.NeevaConstants
import com.neeva.app.UserInfoQuery
import com.neeva.app.billing.billingclient.BillingClientController
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.network.NetworkHandler
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.testcommon.apollo.TestAuthenticatedApolloWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
@OptIn(ExperimentalCoroutinesApi::class)
class NeevaUserTest : BaseTest() {
    @Rule
    @JvmField
    val coroutineScopeRule = CoroutineScopeRule()

    @Mock private lateinit var previewSessionToken: PreviewSessionToken
    @Mock private lateinit var billingClientController: BillingClientController

    private lateinit var context: Context
    private lateinit var apolloWrapper: TestAuthenticatedApolloWrapper
    private lateinit var mockWebLayerModel: WebLayerModel
    private lateinit var neevaConstants: NeevaConstants
    private lateinit var networkHandler: NetworkHandler
    private lateinit var loginToken: LoginToken
    private lateinit var userInfo: UserInfo
    private lateinit var neevaUser: NeevaUser
    private lateinit var sharedPreferencesModel: SharedPreferencesModel

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    override fun setUp() {
        super.setUp()
        // Set up each test to simulate a signed-in user. Calls to fetch() should overwrite UserInfo
        neevaConstants = NeevaConstants()
        context = ApplicationProvider.getApplicationContext()
        networkHandler = mock {
            on { isConnectedToInternet() } doReturn true
        }
        setUpLoggedInUser(context)
        setUpMockWeblayerModel()
        apolloWrapper = TestAuthenticatedApolloWrapper(
            loginToken = loginToken,
            previewSessionToken = previewSessionToken,
            neevaConstants = neevaConstants
        )
        coroutineScopeRule.scope.advanceUntilIdle()
    }

    private fun setUpLoggedInUser(context: Context) {
        sharedPreferencesModel = SharedPreferencesModel(context)
        loginToken = LoginToken(
            coroutineScope = coroutineScopeRule.scope,
            dispatchers = coroutineScopeRule.dispatchers,
            neevaConstants = neevaConstants,
            sharedPreferencesModel = sharedPreferencesModel
        )
        loginToken.updateCachedCookie("myToken")
        userInfo = UserInfo(
            "my-id",
            "some display name",
            "email@neeva.co",
            "https://www.cdn/my-image.png",
            NeevaUser.SSOProvider.GOOGLE.name
        )
        neevaUser = NeevaUserImpl(
            coroutineScope = coroutineScopeRule.scope,
            dispatchers = coroutineScopeRule.dispatchers,
            sharedPreferencesModel = sharedPreferencesModel,
            loginToken = loginToken,
            networkHandler = networkHandler,
            billingClientController = billingClientController
        )
        neevaUser.setUserInfo(userInfo)
    }

    private fun setUpMockWeblayerModel() {
        mockWebLayerModel = mock {}
    }

    @Test
    fun fetch_responseHasErrors_clearsUser() {
        apolloWrapper.registerTestResponse(
            UserInfoQuery(),
            USER_RESPONSE,
            errors = listOf(
                com.apollographql.apollo3.api.Error(
                    message = "login required to access this field",
                    locations = null,
                    path = listOf("user"),
                    extensions = mapOf(
                        "neeva" to mapOf(
                            "code" to "login_required",
                            "userMessage" to "You must be logged in to get this data.",
                            "errorMessage" to "login required to access this field"
                        )
                    ),
                    nonStandardFields = null
                )
            )
        )
        coroutineScopeRule.scope.advanceUntilIdle()
        runBlocking {
            neevaUser.fetch(apolloWrapper, context)
        }
        expectThat(neevaUser.userInfoFlow.value).isEqualTo(null)
    }

    @Test
    fun fetch_noWifi_doesNotClearUser() {
        apolloWrapper.registerTestResponse(UserInfoQuery(), null)
        coroutineScopeRule.scope.advanceUntilIdle()
        networkHandler = mock {
            on { isConnectedToInternet() } doReturn false
        }
        setUpLoggedInUser(context)
        runBlocking {
            neevaUser.fetch(apolloWrapper, context)
        }
        expectThat(neevaUser.userInfoFlow.value).isEqualTo(userInfo)
    }

    @Test
    fun fetch_apolloNetworkError_doesNotClearUser() {
        // An ApolloNetworkError is thrown when the fetch is unable to be executed due to a lack of Wifi.
        apolloWrapper.registerTestResponse(UserInfoQuery(), null)
        coroutineScopeRule.scope.advanceUntilIdle()
        runBlocking {
            neevaUser.fetch(apolloWrapper, context)
        }
        expectThat(neevaUser.userInfoFlow.value).isEqualTo(userInfo)
    }

    @Test
    fun clearUser_dataIsEmpty_clearsNeevaUserInfo() {
        val neevaUser = NeevaUserImpl(
            coroutineScope = coroutineScopeRule.scope,
            dispatchers = coroutineScopeRule.dispatchers,
            sharedPreferencesModel = sharedPreferencesModel,
            loginToken = loginToken,
            networkHandler = networkHandler,
            billingClientController = billingClientController
        )
        neevaUser.clearUserInfo()
        expectThat(neevaUser.userInfoFlow.value).isEqualTo(null)
    }

    @Test
    fun clearUser_dataIsNotEmpty_clearsNeevaUserInfo() {
        neevaUser.clearUserInfo()
        expectThat(neevaUser.userInfoFlow.value).isEqualTo(null)
    }

    @Test
    fun isSignedOut_whenSignedIn_returnsFalse() {
        expectThat(neevaUser.isSignedOut()).isEqualTo(false)
    }

    @Test
    fun fetch_goodResponse_setsUser() {
        apolloWrapper.registerTestResponse(UserInfoQuery(), USER_RESPONSE)
        coroutineScopeRule.scope.advanceUntilIdle()
        runBlocking {
            neevaUser.fetch(apolloWrapper, context)
        }

        expectThat(neevaUser.userInfoFlow.value).isEqualTo(
            UserInfo(
                id = "response_id",
                displayName = "response_displayName",
                email = "response_email",
                pictureURL = "response_pictureUrl",
                ssoProviderString = NeevaUser.SSOProvider.UNKNOWN.name
            )
        )
    }

    companion object {
        val USER_RESPONSE = UserInfoQuery.Data(
            user = UserInfoQuery.User(
                id = "response_id",
                profile = UserInfoQuery.Profile(
                    displayName = "response_displayName",
                    email = "response_email",
                    pictureURL = "response_pictureUrl"
                ),
                flags = emptyList(),
                featureFlags = emptyList(),
                authProvider = null,
                subscription = null,
                subscriptionType = null
            )
        )
    }
}
