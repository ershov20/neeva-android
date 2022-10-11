// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.apollo

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.neeva.app.BaseTest
import com.neeva.app.NeevaConstants
import com.neeva.app.SearchQuery
import com.neeva.app.userdata.LoginToken
import com.neeva.app.userdata.PreviewSessionToken
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isTrue

@RunWith(AndroidJUnit4::class)
class AuthenticatedApolloWrapperTest : BaseTest() {
    @MockK lateinit var apolloClientWrapper: ApolloClientWrapper

    private lateinit var loginToken: LoginToken
    private lateinit var previewToken: PreviewSessionToken

    private lateinit var neevaConstants: NeevaConstants
    private lateinit var apolloWrapper: AuthenticatedApolloWrapper

    private var loginCookie: String = ""
    private var previewCookie: String = ""

    override fun setUp() {
        super.setUp()

        loginToken = mockk {
            coEvery { getOrFetchCookie() } answers { loginCookie }
            every { isEmpty() } answers { loginCookie.isEmpty() }
            every { isNotEmpty() } answers { loginCookie.isNotEmpty() }
        }
        previewToken = mockk {
            coEvery { getOrFetchCookie() } answers { previewCookie }
        }

        neevaConstants = NeevaConstants()
        apolloWrapper = AuthenticatedApolloWrapper(
            loginToken = loginToken,
            previewSessionToken = previewToken,
            neevaConstants = neevaConstants,
            apolloClientWrapper = apolloClientWrapper
        )
    }

    @Test
    fun prepareForOperation_withLoginTokenAndNoPreviewToken_doesNotTryToGetPreviewToken() {
        loginCookie = "not empty"
        previewCookie = ""

        expectThat(
            runBlocking { apolloWrapper.prepareForOperation(userMustBeLoggedIn = false) }
        ).isTrue()
        coVerify(exactly = 0) { previewToken.getOrFetchCookie() }

        expectThat(
            runBlocking { apolloWrapper.prepareForOperation(userMustBeLoggedIn = true) }
        ).isTrue()
        coVerify(exactly = 0) { previewToken.getOrFetchCookie() }
    }

    @Test
    fun prepareForOperation_withLoginTokenUnset_checksForSetPreviewToken() {
        loginCookie = ""
        previewCookie = "preview cookie"

        expectThat(
            runBlocking { apolloWrapper.prepareForOperation(userMustBeLoggedIn = false) }
        ).isTrue()
        coVerify(exactly = 1) { previewToken.getOrFetchCookie() }

        expectThat(
            runBlocking { apolloWrapper.prepareForOperation(userMustBeLoggedIn = true) }
        ).isTrue()
        coVerify(exactly = 2) { previewToken.getOrFetchCookie() }
    }

    @Test
    fun prepareForOperation_withNeitherTokenSet_checksIfUserMustBeLoggedIn() {
        loginCookie = ""
        previewCookie = ""

        expectThat(
            runBlocking { apolloWrapper.prepareForOperation(userMustBeLoggedIn = false) }
        ).isTrue()
        coVerify(exactly = 1) { previewToken.getOrFetchCookie() }

        expectThat(
            runBlocking { apolloWrapper.prepareForOperation(userMustBeLoggedIn = true) }
        ).isFalse()
        coVerify(exactly = 2) { previewToken.getOrFetchCookie() }
    }

    @Test
    fun performQuery_withNeitherTokenSet_requestsPreviewTokenBeforeExecuting() {
        loginCookie = ""
        previewCookie = ""

        val response = mockk<ApolloResponse<SearchQuery.Data>> {
            every { hasErrors() } returns false
        }

        val apolloCall = mockk<ApolloCall<SearchQuery.Data>> {
            coEvery { execute() } returns response
        }

        every { apolloClientWrapper.query<SearchQuery.Data>(any()) } returns apolloCall
        coEvery { previewToken.getOrFetchCookie() } answers {
            // Say that the cookie becomes valid when `getOrFetchCookie()` is called.
            previewCookie = "now valid cookie"
            previewCookie
        }

        runBlocking {
            val result = apolloWrapper.performQuery(
                SearchQuery(query = "query"),
                userMustBeLoggedIn = true
            )
            expectThat(result.response).isEqualTo(response)
            expectThat(result.exception).isNull()
        }

        coVerify(exactly = 1) { previewToken.getOrFetchCookie() }
    }

    @Test
    fun performQuery_ifFailsToRetrieveTokenAndLoginRequired_failsToPerformQuery() {
        loginCookie = ""
        previewCookie = ""

        val response = mockk<ApolloResponse<SearchQuery.Data>> {
            every { hasErrors() } returns false
        }

        val apolloOperation = mockk<Operation<SearchQuery.Data>> {}
        val apolloCall = mockk<ApolloCall<SearchQuery.Data>> {
            coEvery { execute() } returns response
            every { operation } returns apolloOperation
        }

        every { apolloClientWrapper.query<SearchQuery.Data>(any()) } returns apolloCall
        coEvery { previewToken.getOrFetchCookie() } answers {
            // Say that the cookie stays invalid.
            previewCookie = ""
            previewCookie
        }

        runBlocking {
            val result = apolloWrapper.performQuery(
                SearchQuery(query = "query"),
                userMustBeLoggedIn = true
            )
            expectThat(result.response).isNull()
        }

        coVerify(exactly = 1) { previewToken.getOrFetchCookie() }
    }
}
