package com.neeva.app.apollo

import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Query

interface ApolloWrapper {
    suspend fun <D : Query.Data> performQuery(
        query: Query<D>,
        userMustBeLoggedIn: Boolean
    ): ApolloResponse<D>?

    fun <D : Mutation.Data> performMutationAsync(
        mutation: Mutation<D>,
        userMustBeLoggedIn: Boolean,
        callback: (ApolloResponse<D>?) -> Unit
    )

    suspend fun <D : Mutation.Data> performMutation(
        mutation: Mutation<D>,
        userMustBeLoggedIn: Boolean
    ): ApolloResponse<D>?
}
