package com.neeva.testcommon.apollo

import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.testing.MapTestNetworkTransport
import com.apollographql.apollo3.testing.registerTestResponse
import com.neeva.app.NeevaConstants
import com.neeva.app.apollo.ApolloClientWrapper
import com.neeva.app.apollo.AuthenticatedApolloWrapper
import com.neeva.app.apollo.UnauthenticatedApolloWrapper
import com.neeva.app.userdata.NeevaUserToken

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
class TestAuthenticatedApolloWrapper(
    neevaUserToken: NeevaUserToken,
    neevaConstants: NeevaConstants,
    val testApolloClientWrapper: TestApolloClientWrapper = TestApolloClientWrapper()
) : AuthenticatedApolloWrapper(
    neevaUserToken = neevaUserToken,
    neevaConstants = neevaConstants,
    apolloClientWrapper = testApolloClientWrapper
) {
    fun <D : Operation.Data> registerTestResponse(
        operation: Operation<D>,
        response: D?,
        errors: List<Error>? = null
    ) {
        testApolloClientWrapper.apolloClient().registerTestResponse(operation, response, errors)
    }
}

@OptIn(ApolloExperimental::class)
class TestUnauthenticatedApolloWrapper(
    neevaConstants: NeevaConstants,
    val testApolloClientWrapper: TestApolloClientWrapper = TestApolloClientWrapper()
) : UnauthenticatedApolloWrapper(
    neevaConstants = neevaConstants,
    apolloClientWrapper = testApolloClientWrapper
) {
    fun <D : Operation.Data> registerTestResponse(operation: Operation<D>, response: D?) {
        testApolloClientWrapper.apolloClient().registerTestResponse(operation, response)
    }
}
