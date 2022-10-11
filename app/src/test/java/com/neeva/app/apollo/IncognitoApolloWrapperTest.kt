// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.apollo

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.api.ApolloResponse
import com.neeva.app.BaseTest
import com.neeva.app.NeevaConstants
import com.neeva.app.SearchQuery
import com.neeva.app.userdata.IncognitoSessionToken
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.isTrue

@RunWith(AndroidJUnit4::class)
class IncognitoApolloWrapperTest : BaseTest() {
    @MockK lateinit var apolloClientWrapper: ApolloClientWrapper

    private lateinit var incognitoSessionToken: IncognitoSessionToken
    private lateinit var neevaConstants: NeevaConstants
    private lateinit var apolloWrapper: IncognitoApolloWrapper

    private var incognitoCookie: String = ""

    override fun setUp() {
        super.setUp()

        incognitoSessionToken = mockk {
            coEvery { getCurrentCookieValue() } answers { incognitoCookie }
        }

        neevaConstants = NeevaConstants()
        apolloWrapper = IncognitoApolloWrapper(
            incognitoSessionToken = incognitoSessionToken,
            neevaConstants = neevaConstants,
            apolloClientWrapper = apolloClientWrapper
        )
    }

    @Test
    fun prepareForOperation_withSetToken_doesNotRequest() {
        incognitoCookie = "not empty"

        val result = runBlocking {
            apolloWrapper.prepareForOperation(userMustBeLoggedIn = false)
        }
        expectThat(result).isTrue()

        verify(exactly = 0) { incognitoSessionToken.requestNewCookie() }
        coVerify(exactly = 0) { incognitoSessionToken.waitForRequest() }
    }

    @Test
    fun prepareForOperation_withNoToken_performsRequest() {
        incognitoCookie = ""
        every { incognitoSessionToken.requestNewCookie() } returns Unit
        coEvery { incognitoSessionToken.waitForRequest() } returns "unused"

        val result = runBlocking {
            apolloWrapper.prepareForOperation(userMustBeLoggedIn = false)
        }
        expectThat(result).isTrue()

        verify(exactly = 1) { incognitoSessionToken.requestNewCookie() }
        coVerify(exactly = 1) { incognitoSessionToken.waitForRequest() }
    }

    @Test
    fun performQuery_withValidToken_executes() {
        incognitoCookie = "cookie"

        val response = mockk<ApolloResponse<SearchQuery.Data>> {
            every { hasErrors() } returns false
        }

        val apolloCall = mockk<ApolloCall<SearchQuery.Data>> {
            coEvery { execute() } returns response
        }

        every { apolloClientWrapper.query<SearchQuery.Data>(any()) } returns apolloCall

        runBlocking {
            apolloWrapper.performQuery(
                SearchQuery(query = "query"),
                userMustBeLoggedIn = true
            )
        }

        verify(exactly = 0) { incognitoSessionToken.requestNewCookie() }
        coVerify(exactly = 0) { incognitoSessionToken.waitForRequest() }
    }

    @Test
    fun performQuery_withInvalidToken_requestsTokenThenExecutes() {
        val response = mockk<ApolloResponse<SearchQuery.Data>> {
            every { hasErrors() } returns false
        }

        val apolloCall = mockk<ApolloCall<SearchQuery.Data>> {
            coEvery { execute() } returns response
        }

        every { apolloClientWrapper.query<SearchQuery.Data>(any()) } returns apolloCall
        every { incognitoSessionToken.requestNewCookie() } answers {
            // Say that the token is now valid.
            incognitoCookie = "now valid cookie"
        }
        coEvery { incognitoSessionToken.waitForRequest() } returns "unused"

        runBlocking {
            apolloWrapper.performQuery(
                SearchQuery(query = "query"),
                userMustBeLoggedIn = true
            )
        }

        verify(exactly = 1) { incognitoSessionToken.requestNewCookie() }
        coVerify(exactly = 1) { incognitoSessionToken.waitForRequest() }
    }
}
