package com.neeva.app.apollo

import android.util.Log
import androidx.annotation.CallSuper
import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.exception.ApolloException

/** Manages an Apollo client that can be used to fire queries and mutations at the Neeva backend. */
abstract class BaseApolloWrapper(
    private val apolloClientWrapper: ApolloClientWrapper
) : ApolloWrapper {
    /** Returns whether or not the operation is allowed to be performed. */
    abstract fun mayPerformOperation(userMustBeLoggedIn: Boolean): Boolean

    @CallSuper
    override suspend fun <D : Query.Data> performQuery(
        query: Query<D>,
        userMustBeLoggedIn: Boolean
    ): ApolloResponse<D>? {
        return performOperation(apolloClientWrapper.query(query), userMustBeLoggedIn)
    }

    @CallSuper
    override suspend fun <D : Mutation.Data> performMutation(
        mutation: Mutation<D>,
        userMustBeLoggedIn: Boolean
    ): ApolloResponse<D>? {
        return performOperation(apolloClientWrapper.mutation(mutation), userMustBeLoggedIn)
    }

    private suspend fun <D : Operation.Data> performOperation(
        call: ApolloCall<D>,
        userMustBeLoggedIn: Boolean
    ): ApolloResponse<D>? {
        if (!mayPerformOperation(userMustBeLoggedIn)) {
            Log.w(TAG, "Not allowed to perform operation: ${call.operation}")
            return null
        }

        return try {
            val response = call.execute()
            if (response.hasErrors()) {
                Log.e(TAG, "Response had errors: ${call.operation}")
                response.errors?.forEach { Log.e(TAG, "\tError: ${it.message}") }
                null
            } else {
                response
            }
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Could not perform operation", e)
            null
        } catch (e: ApolloException) {
            Log.e(TAG, "Could not perform operation", e)
            null
        }
    }

    companion object {
        private const val TAG = "BaseApolloWrapper"
    }
}
