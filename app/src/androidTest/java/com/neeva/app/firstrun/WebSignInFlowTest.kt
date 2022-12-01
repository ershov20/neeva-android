// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.firstrun

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.performTextInput
import com.neeva.app.BaseBrowserTest
import com.neeva.app.NeevaActivity
import com.neeva.app.NeevaConstants
import com.neeva.app.PresetSharedPreferencesRule
import com.neeva.app.R
import com.neeva.app.TestOktaModule
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.clickOnNodeWithText
import com.neeva.app.getString
import com.neeva.app.openOverflowMenuAndClickItem
import com.neeva.app.performTextInput
import com.neeva.app.startActivity
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.waitForNavDestination
import com.neeva.app.waitForNode
import com.neeva.app.waitForNodeWithText
import com.neeva.app.waitForUrl
import com.squareup.moshi.Moshi
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule
import org.junit.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@HiltAndroidTest
class WebSignInFlowTest : BaseBrowserTest() {
    @get:Rule(order = 10000)
    val androidComposeRule = createAndroidComposeRule<NeevaActivity>()

    @get:Rule
    val sharedPreferencesRule = PresetSharedPreferencesRule(
        useCustomTabsForLogin = false
    )

    @Inject lateinit var neevaConstants: NeevaConstants
    @Inject lateinit var oktaSignUpHandler: OktaSignUpHandler

    override fun setUp() {
        super.setUp()
        androidComposeRule.apply { startActivity() }
    }

    @Test
    fun signInUsingGoogle() {
        androidComposeRule.apply {
            openOverflowMenuAndClickItem(R.string.settings)
            waitForNavDestination(AppNavDestination.SETTINGS)

            clickOnNodeWithText(getString(R.string.settings_sign_in_to_join_neeva))
            clickOnNodeWithText(getString(R.string.already_have_account), substring = true)
            clickOnNodeWithText(getString(R.string.sign_in_with_google))
            waitForNavDestination(AppNavDestination.BROWSER)

            val expectedUri = FirstRunModel.getWebAuthUri(
                neevaConstants = neevaConstants,
                signup = false,
                provider = NeevaUser.SSOProvider.GOOGLE,
                oktaLoginHint = ""
            )
            waitForUrl(expectedUri.toString())
        }
    }

    @Test
    fun signInUsingMicrosoft() {
        androidComposeRule.apply {
            openOverflowMenuAndClickItem(R.string.settings)
            waitForNavDestination(AppNavDestination.SETTINGS)

            clickOnNodeWithText(getString(R.string.settings_sign_in_to_join_neeva))
            clickOnNodeWithText(getString(R.string.already_have_account), substring = true)
            clickOnNodeWithText(getString(R.string.sign_in_with_microsoft))
            waitForNavDestination(AppNavDestination.BROWSER)

            val expectedUri = FirstRunModel.getWebAuthUri(
                neevaConstants = neevaConstants,
                signup = false,
                provider = NeevaUser.SSOProvider.MICROSOFT,
                oktaLoginHint = ""
            )
            waitForUrl(expectedUri.toString())
        }
    }

    @Test
    fun signInUsingOtherOptions() {
        androidComposeRule.apply {
            openOverflowMenuAndClickItem(R.string.settings)
            waitForNavDestination(AppNavDestination.SETTINGS)

            clickOnNodeWithText(getString(R.string.settings_sign_in_to_join_neeva))
            clickOnNodeWithText(getString(R.string.already_have_account), substring = true)
            waitForNode(hasText(getString(R.string.email_label))).performTextInput("test@neeva.com")

            clickOnNodeWithText(getString(R.string.sign_in_with_okta))
            waitForNavDestination(AppNavDestination.BROWSER)

            val expectedUri = FirstRunModel.getWebAuthUri(
                neevaConstants = neevaConstants,
                signup = false,
                provider = NeevaUser.SSOProvider.OKTA,
                oktaLoginHint = "test@neeva.com"
            )
            waitForUrl(expectedUri.toString())
        }
    }

    @Test
    fun signUpUsingGoogle() {
        androidComposeRule.apply {
            openOverflowMenuAndClickItem(R.string.settings)
            waitForNavDestination(AppNavDestination.SETTINGS)

            clickOnNodeWithText(getString(R.string.settings_sign_in_to_join_neeva))
            clickOnNodeWithText(getString(R.string.sign_up_with_google))
            waitForNavDestination(AppNavDestination.BROWSER)

            val expectedUri = FirstRunModel.getWebAuthUri(
                neevaConstants = neevaConstants,
                signup = true,
                provider = NeevaUser.SSOProvider.GOOGLE,
                oktaLoginHint = ""
            )
            waitForUrl(expectedUri.toString())
        }
    }

    @Test
    fun signUpUsingOkta_withReturnedCookie_processesCookie() {
        val server = MockWebServer()
        server.enqueue(
            MockResponse()
                .setBody("")
                .addHeader("Set-Cookie:${neevaConstants.loginCookieKey}=cookieValue")
        )
        server.start()

        // Override where the Okta signup flow fires its network request to, as well as where it
        // sends the user to once a login cookie is successfully sent.
        val testOktaSignUpHandler = oktaSignUpHandler as TestOktaModule.TestOktaSignUpHandler
        testOktaSignUpHandler.overrideCreateOktaAccountUrl = server.url("/login/path").toString()
        testOktaSignUpHandler.overrideOnLoginCookieReceivedUrl = neevaConstants.appURL + "/cookie="

        androidComposeRule.apply {
            openOverflowMenuAndClickItem(R.string.settings)
            waitForNavDestination(AppNavDestination.SETTINGS)

            clickOnNodeWithText(getString(R.string.settings_sign_in_to_join_neeva))
            clickOnNodeWithText(getString(R.string.sign_up_other_options))
            waitForIdle()

            performTextInput(hasText(getString(R.string.email_label)), "test@neeva.com")
            performTextInput(hasText(getString(R.string.password_label)), "hunter2")
            waitForIdle()

            clickOnNodeWithText(getString(R.string.sign_up_with_okta))
            waitForNavDestination(AppNavDestination.BROWSER)

            waitForUrl(testOktaSignUpHandler.overrideOnLoginCookieReceivedUrl + "cookieValue")
        }

        // It'd be nice to confirm that the email and the password are both in the request body, but
        // the request salts the password so we can't make the comparison in a straightforward way.
        val recordedRequest = server.takeRequest()
        val requestBody = Moshi.Builder().build()
            .adapter(OktaSignupRequestParams::class.java)
            .fromJson(recordedRequest.body.readUtf8())
        expectThat(requestBody!!.email).isEqualTo("test@neeva.com")
    }

    @Test
    fun signUpUsingOkta_withoutReturnedCookie_showsError() {
        val server = MockWebServer()
        server.enqueue(MockResponse().setBody(""))
        server.start()

        // Override where the Okta signup flow fires its network request to, as well as where it
        // sends the user to once a login cookie is successfully sent.
        val testOktaSignUpHandler = oktaSignUpHandler as TestOktaModule.TestOktaSignUpHandler
        testOktaSignUpHandler.overrideCreateOktaAccountUrl = server.url("/login/path").toString()
        testOktaSignUpHandler.overrideOnLoginCookieReceivedUrl = neevaConstants.appURL + "/cookie="

        androidComposeRule.apply {
            openOverflowMenuAndClickItem(R.string.settings)
            waitForNavDestination(AppNavDestination.SETTINGS)

            clickOnNodeWithText(getString(R.string.settings_sign_in_to_join_neeva))
            clickOnNodeWithText(getString(R.string.sign_up_other_options))
            waitForIdle()

            performTextInput(hasText(getString(R.string.email_label)), "test@neeva.com")
            performTextInput(hasText(getString(R.string.password_label)), "hunter2")
            waitForIdle()

            clickOnNodeWithText(getString(R.string.sign_up_with_okta))
            waitForNodeWithText(getString(R.string.generic_signup_error))
        }

        // It'd be nice to confirm that the email and the password are both in the request body, but
        // the request salts the password so we can't make the comparison in a straightforward way.
        val recordedRequest = server.takeRequest()
        val requestBody = Moshi.Builder().build()
            .adapter(OktaSignupRequestParams::class.java)
            .fromJson(recordedRequest.body.readUtf8())
        expectThat(requestBody!!.email).isEqualTo("test@neeva.com")
    }
}
