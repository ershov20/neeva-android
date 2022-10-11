// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.userdata

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.neeva.app.BaseTest
import com.neeva.app.CoroutineScopeRule
import com.neeva.app.NeevaConstants
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import okhttp3.Response
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.chromium.weblayer.Browser
import org.chromium.weblayer.Callback
import org.chromium.weblayer.CookieChangeCause
import org.chromium.weblayer.CookieChangedCallback
import org.chromium.weblayer.CookieManager
import org.chromium.weblayer.Profile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class SessionTokenTest : BaseTest() {
    @get:Rule val coroutineScopeRule = CoroutineScopeRule()

    @MockK lateinit var cancelRunnable: Runnable

    private lateinit var neevaConstants: NeevaConstants
    private lateinit var sessionToken: SessionToken

    private lateinit var cookieManager: CookieManager
    private lateinit var profile: Profile
    private lateinit var browser: Browser

    private lateinit var server: MockWebServer
    private lateinit var serverUrl: String

    override fun setUp() {
        super.setUp()

        neevaConstants = NeevaConstants()

        cookieManager = mockk {
            every { addCookieChangedCallback(any(), any(), any()) } returns cancelRunnable

            every {
                getCookie(
                    eq(Uri.parse(neevaConstants.appURL)),
                    any()
                )
            } returns Unit
        }

        profile = mockk {
            every { cookieManager } returns this@SessionTokenTest.cookieManager
        }

        browser = mockk {
            every { isDestroyed } returns false
            every { profile } returns this@SessionTokenTest.profile
        }
    }

    private fun initializeSessionToken(
        initialCookieValue: String,
        serverResponse: String? = null
    ) {
        server = MockWebServer()
        serverResponse?.let { server.enqueue(MockResponse().setBody(it)) }
        server.start()
        serverUrl = server.url("/test/endpoint").toString()

        sessionToken = object : SessionToken(
            coroutineScope = coroutineScopeRule.scope,
            dispatchers = coroutineScopeRule.dispatchers,
            neevaConstants = neevaConstants,
            endpointURL = serverUrl,
            cookieName = neevaConstants.previewCookieKey
        ) {
            override var cachedValue: String = initialCookieValue

            override suspend fun processResponse(response: Response): Boolean {
                cachedValue = response.body?.string() ?: ""
                return true
            }

            override fun updateCachedCookie(newValue: String) {
                cachedValue = newValue
            }
        }
    }

    @Test
    fun cookieChangedCallback_withLiveBrowser_copiesCookieValue() {
        initializeSessionToken(initialCookieValue = "")
        val expectedCookieString = "${neevaConstants.previewCookieKey}=test cookie value"

        sessionToken.initializeCookieManager(browser, requestCookieIfEmpty = false)

        // The SessionToken should have hooked into the CookieManager.
        val callbackSlot = CapturingSlot<CookieChangedCallback>()
        verify {
            cookieManager.addCookieChangedCallback(
                eq(Uri.parse(neevaConstants.appURL)),
                eq(neevaConstants.previewCookieKey),
                capture(callbackSlot)
            )
        }

        // Fire the CookieChangedCallback so that the SessionToken asks the CookieManager for the
        // current state of the cookie.
        callbackSlot.captured.onCookieChanged("unused", CookieChangeCause.INSERTED)
        coroutineScopeRule.advanceUntilIdle()

        // Fire the callback with a cookie string that has two different cookies in it.
        val getCookieCallbackSlot = mutableListOf<Callback<String>>()
        verify {
            cookieManager.getCookie(
                eq(Uri.parse(neevaConstants.appURL)),
                capture(getCookieCallbackSlot)
            )
        }
        getCookieCallbackSlot.last().onResult(
            "unusedcookie=unusedvalue;$expectedCookieString"
        )
        coroutineScopeRule.advanceUntilIdle()

        // We should have received and cached the cookie.
        expectThat(sessionToken.cachedValue).isEqualTo("test cookie value")
    }

    @Test
    fun cookieChangedCallback_withLiveBrowserAndMissingCookie_clearsCachedCookie() {
        initializeSessionToken(initialCookieValue = "preset")

        // Expect that the Browser is initialized with the "preset" value.
        sessionToken.initializeCookieManager(browser, requestCookieIfEmpty = false)
        coroutineScopeRule.advanceUntilIdle()

        // The SessionToken should have hooked into the CookieManager via a CookieChangedCallback.
        val callbackSlot = CapturingSlot<CookieChangedCallback>()
        verify {
            cookieManager.addCookieChangedCallback(
                eq(Uri.parse(neevaConstants.appURL)),
                eq(neevaConstants.previewCookieKey),
                capture(callbackSlot)
            )
        }

        // Fire the CookieChangedCallback so that the SessionToken asks the CookieManager for the
        // current state of the cookie.
        callbackSlot.captured.onCookieChanged("unused", CookieChangeCause.INSERTED)
        coroutineScopeRule.advanceUntilIdle()

        // Fire the callback with a cookie string that doesn't have the session cookie.
        val getCookieCallbackSlot = mutableListOf<Callback<String>>()
        verify {
            cookieManager.getCookie(
                eq(Uri.parse(neevaConstants.appURL)),
                capture(getCookieCallbackSlot)
            )
        }
        getCookieCallbackSlot.last().onResult("unusedcookie=unusedvalue")
        coroutineScopeRule.advanceUntilIdle()

        // We should have been told no cookie exists and cleared it out.
        expectThat(sessionToken.cachedValue).isEmpty()
    }

    @Test
    fun cookieChangedCallback_withDeadBrowser_doesNothingInCookieChangedCallback() {
        initializeSessionToken(initialCookieValue = "")
        sessionToken.initializeCookieManager(browser, requestCookieIfEmpty = false)

        // The SessionToken should have hooked into the CookieManager.
        val callbackSlot = CapturingSlot<CookieChangedCallback>()
        verify {
            cookieManager.addCookieChangedCallback(
                eq(Uri.parse(neevaConstants.appURL)),
                eq(neevaConstants.previewCookieKey),
                capture(callbackSlot)
            )
        }

        // Say that the browser is dead.
        every { browser.isDestroyed } returns true

        // Fire the CookieChangedCallback so that the SessionToken asks the CookieManager for the
        // current state of the cookie.
        callbackSlot.captured.onCookieChanged("unused", CookieChangeCause.INSERTED)
        coroutineScopeRule.advanceUntilIdle()

        // We shouldn't have tried to do anything with the CookieManager.
        verify(exactly = 0) { cookieManager.getCookie(any(), any()) }
        verify(exactly = 0) { cookieManager.setCookie(any(), any(), any()) }
    }

    @Test
    fun requestNewCookie_withValidResponse_savesCookie() {
        // Initialize the cookie with an empty value so that we try to fetch one.
        initializeSessionToken(initialCookieValue = "", serverResponse = "response processed")
        sessionToken.initializeCookieManager(browser, requestCookieIfEmpty = true)
        coroutineScopeRule.advanceUntilIdle()

        // Fire the callback with a cookie string that doesn't have the session cookie in it.
        val getCookieCallbackSlot = CapturingSlot<Callback<String>>()
        verify {
            cookieManager.getCookie(
                eq(Uri.parse(neevaConstants.appURL)),
                capture(getCookieCallbackSlot)
            )
        }

        // After the callback is fired, we'll expect the Browser to be updated with the new value.
        every {
            cookieManager.setCookie(
                eq(Uri.parse(neevaConstants.appURL)),
                eq("${neevaConstants.previewCookieKey}=response processed"),
                any()
            )
        } returns Unit

        getCookieCallbackSlot.captured.onResult("unusedcookie=unusedvalue;")
        coroutineScopeRule.advanceUntilIdle()

        // Confirm that we hit the right endpoint.
        val recordedRequest = server.takeRequest()
        expectThat(recordedRequest.requestUrl?.toString()).isEqualTo(serverUrl)

        // The cookie should be set correctly.
        expectThat(sessionToken.cachedValue).isEqualTo("response processed")
    }

    @Test
    fun requestNewCookie_withNoResponse_doesNothing() {
        // Initialize the cookie with an empty value so that we try to fetch one.
        initializeSessionToken(initialCookieValue = "", serverResponse = null)
        sessionToken.initializeCookieManager(browser, requestCookieIfEmpty = true)
        coroutineScopeRule.advanceUntilIdle()

        // Fire the callback with a cookie string that doesn't have the session cookie in it.
        val getCookieCallbackSlot = CapturingSlot<Callback<String>>()
        verify {
            cookieManager.getCookie(
                eq(Uri.parse(neevaConstants.appURL)),
                capture(getCookieCallbackSlot)
            )
        }

        // After the callback is fired, we'll expect the Browser to be updated with the new value.
        getCookieCallbackSlot.captured.onResult("unusedcookie=unusedvalue;")
        coroutineScopeRule.advanceUntilIdle()

        // Confirm that we hit the right endpoint.
        val recordedRequest = server.takeRequest()
        expectThat(recordedRequest.requestUrl?.toString()).isEqualTo(serverUrl)

        // The cookie should still be empty.
        expectThat(sessionToken.cachedValue).isEqualTo("")
    }
}
