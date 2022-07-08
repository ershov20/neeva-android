package com.neeva.app.apollo

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.network.okHttpClient
import com.neeva.app.NeevaConstants
import com.neeva.app.userdata.NeevaUserToken
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import org.mockito.kotlin.mock
import strikt.api.expectThat
import strikt.assertions.isEmpty

class TestApolloWrapper(
    private val testInterceptor: TestInterceptor = TestInterceptor(),
    neevaUserToken: NeevaUserToken = mock(),
    neevaConstants: NeevaConstants = NeevaConstants()
) : AuthenticatedApolloWrapper(
    neevaUserToken = neevaUserToken,
    neevaConstants = neevaConstants,
    apolloClientWrapper = object : ApolloClientWrapper {
        val apolloClient = ApolloClient.Builder()
            .serverUrl("https://fake.url")
            .okHttpClient(
                OkHttpClient.Builder()
                    .addInterceptor(testInterceptor)
                    .build()
            )
            .build()

        override fun apolloClient() = apolloClient
    }
) {
    val performedQueries = mutableListOf<Any>()
    val performedMutations = mutableListOf<Any>()

    override suspend fun <D : Query.Data> performQuery(
        query: Query<D>,
        userMustBeLoggedIn: Boolean
    ): ApolloResponse<D>? {
        performedQueries.add(query)
        return super.performQuery(query, userMustBeLoggedIn)
    }

    override suspend fun <D : Mutation.Data> performMutation(
        mutation: Mutation<D>,
        userMustBeLoggedIn: Boolean
    ): ApolloResponse<D>? {
        performedMutations.add(mutation)
        return super.performMutation(mutation, userMustBeLoggedIn)
    }

    fun tearDown() {
        // We expect that all responses that we add are used.
        expectThat(testInterceptor.responses).isEmpty()
    }

    fun addResponse(response: String) = testInterceptor.responses.add(response)

    /**
     * Manages a queue of JSON strings that are returned as the ApolloWrapper is asked to fire
     * off queries and mutations to the backend.
     *
     * If not enough responses are provided, 503s are returned.
     */
    class TestInterceptor : Interceptor {
        val responses = mutableListOf<String>()

        override fun intercept(chain: Interceptor.Chain): Response {
            val currentResponse = responses.removeFirstOrNull()

            if (currentResponse != null) {
                val responseBody = ResponseBody.create(
                    "application/json".toMediaTypeOrNull(),
                    currentResponse
                )

                return Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_2)
                    .message("Success")
                    .code(200)
                    .body(responseBody)
                    .addHeader("content-type", "application/json")
                    .build()
            } else {
                val responseBody = ResponseBody.create(
                    "application/json".toMediaTypeOrNull(),
                    "{}"
                )

                return Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_2)
                    .message("Error")
                    .code(503)
                    .body(responseBody)
                    .addHeader("content-type", "application/json")
                    .build()
            }
        }
    }
}
