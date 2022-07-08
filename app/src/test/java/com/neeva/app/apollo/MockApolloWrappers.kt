package com.neeva.app.apollo

import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.testing.MapTestNetworkTransport
import com.apollographql.apollo3.testing.registerTestResponse
import com.neeva.app.NeevaConstants
import com.neeva.app.userdata.NeevaUserToken
import javax.inject.Inject

@OptIn(ApolloExperimental::class)
class TestApolloClientWrapper : ApolloClientWrapper {
    val performedOperations = mutableListOf<Any>()

    private val apolloClient = ApolloClient.Builder()
        .networkTransport(MapTestNetworkTransport())
        .build()

    override fun apolloClient(): ApolloClient = apolloClient

    override fun <D : Query.Data> query(query: Query<D>): ApolloCall<D> {
        performedOperations.add(query)
        return super.query(query)
    }

    override fun <D : Mutation.Data> mutation(mutation: Mutation<D>): ApolloCall<D> {
        performedOperations.add(mutation)
        return super.mutation(mutation)
    }
}

@OptIn(ApolloExperimental::class)
class TestAuthenticatedApolloWrapper @Inject constructor(
    neevaUserToken: NeevaUserToken,
    neevaConstants: NeevaConstants
) : AuthenticatedApolloWrapper(
    neevaUserToken = neevaUserToken,
    neevaConstants = neevaConstants,
    apolloClientWrapper = TestApolloClientWrapper()
) {
    val testApolloClientWrapper: TestApolloClientWrapper
        get() { return apolloClientWrapper as TestApolloClientWrapper }

    fun <D : Operation.Data> registerTestResponse(operation: Operation<D>, response: D?) {
        apolloClientWrapper.apolloClient().registerTestResponse(operation, response)
    }
}

@OptIn(ApolloExperimental::class)
class TestUnauthenticatedApolloWrapper @Inject constructor(
    neevaConstants: NeevaConstants
) : UnauthenticatedApolloWrapper(
    neevaConstants = neevaConstants,
    apolloClientWrapper = TestApolloClientWrapper()
) {
    fun <D : Operation.Data> registerTestResponse(operation: Operation<D>, response: D?) {
        apolloClientWrapper.apolloClient().registerTestResponse(operation, response)
    }
}
