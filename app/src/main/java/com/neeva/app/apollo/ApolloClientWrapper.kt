// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.apollo

import androidx.annotation.CallSuper
import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.network.okHttpClient
import com.neeva.app.NeevaConstants
import okhttp3.Cookie

/** Abstracts away calls made to an [ApolloClient]. */
interface ApolloClientWrapper {
    fun apolloClient(): ApolloClient

    @CallSuper
    fun <D : Query.Data> query(query: Query<D>): ApolloCall<D> {
        return apolloClient().query(query)
    }

    @CallSuper
    fun <D : Mutation.Data> mutation(mutation: Mutation<D>): ApolloCall<D> {
        return apolloClient().mutation(mutation)
    }
}

/** Manages an ApolloClient that uses OkHttp to talk to a server. */
class OkHttpApolloClientWrapper(
    neevaConstants: NeevaConstants,
    createAdditionalCookies: () -> List<Cookie> = { emptyList() }
) : ApolloClientWrapper {
    private val neevaOkHttpClient = NeevaOkHttpClient(
        neevaConstants = neevaConstants,
        createAdditionalCookies = createAdditionalCookies
    )

    private val apolloClient = ApolloClient.Builder()
        .serverUrl(neevaConstants.apolloURL)
        .okHttpClient(neevaOkHttpClient.client)
        .build()

    override fun apolloClient() = apolloClient
}
