// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.apollo

import android.util.Log
import androidx.annotation.CallSuper
import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloNetworkException

/** Manages an Apollo client that can be used to fire queries and mutations at the Neeva backend. */
abstract class BaseApolloWrapper(
    private val apolloClientWrapper: ApolloClientWrapper
) : ApolloWrapper {
    /**
     * Prepares to run an operation (e.g. fetches a session token if we don't have one).
     *
     * Returns false if the operation cannot be performed.
     */
    abstract suspend fun prepareForOperation(userMustBeLoggedIn: Boolean): Boolean

    @CallSuper
    override suspend fun <D : Query.Data> performQuery(
        query: Query<D>,
        userMustBeLoggedIn: Boolean
    ): ApolloResponseSummary<D>? {
        return performOperation(apolloClientWrapper.query(query), userMustBeLoggedIn)
    }

    @CallSuper
    override suspend fun <D : Mutation.Data> performMutation(
        mutation: Mutation<D>,
        userMustBeLoggedIn: Boolean
    ): ApolloResponseSummary<D>? {
        return performOperation(apolloClientWrapper.mutation(mutation), userMustBeLoggedIn)
    }

    private suspend fun <D : Operation.Data> performOperation(
        call: ApolloCall<D>,
        userMustBeLoggedIn: Boolean
    ): ApolloResponseSummary<D> {
        if (!prepareForOperation(userMustBeLoggedIn)) {
            Log.w(TAG, "Not allowed to perform operation: ${call.operation}")
            return ApolloResponseSummary(null, null)
        }

        return try {
            val response = call.execute()
            if (response.hasErrors()) {
                Log.e(TAG, "Response had errors: ${call.operation}")
                response.errors?.forEach { Log.e(TAG, "\tError: ${it.message}") }
            }
            ApolloResponseSummary(response, null)
        } catch (e: ApolloNetworkException) {
            Log.e(TAG, "Could not perform operation", e)
            ApolloResponseSummary(null, e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Could not perform operation", e)
            ApolloResponseSummary(null, e)
        } catch (e: ApolloException) {
            Log.e(TAG, "Could not perform operation", e)
            ApolloResponseSummary(null, e)
        }
    }

    companion object {
        private const val TAG = "BaseApolloWrapper"
    }
}
