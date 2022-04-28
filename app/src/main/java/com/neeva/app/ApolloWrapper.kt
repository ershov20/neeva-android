package com.neeva.app

import android.util.Log
import androidx.annotation.CallSuper
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.network.okHttpClient
import com.neeva.app.NeevaConstants.appHost
import com.neeva.app.NeevaConstants.browserTypeCookie
import com.neeva.app.NeevaConstants.browserVersionCookie
import com.neeva.app.NeevaConstants.loginCookie
import com.neeva.app.userdata.NeevaUserToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

/** Authenticated version of [ApolloWrapper] that sends the user token */
open class AuthenticatedApolloWrapper(
    private val neevaUserToken: NeevaUserToken,
    _apolloClient: ApolloClient? = null,
    coroutineScope: CoroutineScope,
    dispatchers: Dispatchers
) : ApolloWrapper(
    _apolloClient = _apolloClient,
    coroutineScope = coroutineScope,
    dispatchers = dispatchers
) {
    override fun loadForRequest(url: HttpUrl): MutableList<Cookie> {
        val cookies = super.loadForRequest(url)
        val token = neevaUserToken.getToken() ?: return cookies
        if (token.isNotEmpty()) {
            val authCookie = Cookie.Builder().name(loginCookie).secure()
                .domain(appHost).expiresAt(Long.MAX_VALUE).value(token).build()
            cookies.add(authCookie)
        }
        return cookies
    }

    override suspend fun <D : Mutation.Data> performMutation(
        mutation: Mutation<D>,
        userMustBeLoggedIn: Boolean
    ): ApolloResponse<D>? {
        if (userMustBeLoggedIn && neevaUserToken.getToken().isEmpty()) {
            Log.i(TAG, "Could not perform mutation because user was not logged in: $mutation")
            return null
        }
        return super.performMutation(mutation, userMustBeLoggedIn)
    }

    override suspend fun <D : Query.Data> performQuery(
        query: Query<D>,
        userMustBeLoggedIn: Boolean
    ): ApolloResponse<D>? {
        if (userMustBeLoggedIn && neevaUserToken.getToken().isEmpty()) {
            Log.i(TAG, "Could not perform query because user was not logged in: $query")
            return null
        }
        return super.performQuery(query, userMustBeLoggedIn)
    }
}

/** Unauthenticated version of [ApolloWrapper] that only send browser cookies and no user token */
open class UnauthenticatedApolloWrapper(
    _apolloClient: ApolloClient? = null,
    coroutineScope: CoroutineScope,
    dispatchers: Dispatchers
) : ApolloWrapper(
    _apolloClient = _apolloClient,
    coroutineScope = coroutineScope,
    dispatchers = dispatchers
) {
    override suspend fun <D : Mutation.Data> performMutation(
        mutation: Mutation<D>,
        userMustBeLoggedIn: Boolean
    ): ApolloResponse<D>? {
        if (userMustBeLoggedIn) {
            Log.i(TAG, "Could not perform mutation, it requires logged in user: $mutation")
            return null
        }
        return super.performMutation(mutation, userMustBeLoggedIn)
    }

    override suspend fun <D : Query.Data> performQuery(
        query: Query<D>,
        userMustBeLoggedIn: Boolean
    ): ApolloResponse<D>? {
        if (userMustBeLoggedIn) {
            Log.i(TAG, "Could not perform query, it requires logged in user: $query")
            return null
        }
        return super.performQuery(query, userMustBeLoggedIn)
    }
}

/** Manages an Apollo client that can be used to fire queries and mutations at the Neeva backend. */
abstract class ApolloWrapper(
    _apolloClient: ApolloClient? = null,
    val coroutineScope: CoroutineScope,
    val dispatchers: Dispatchers
) : CookieJar, Interceptor {
    val apolloClient: ApolloClient = _apolloClient ?: createApolloClient()

    private fun createApolloClient(): ApolloClient {
        return ApolloClient.Builder()
            .serverUrl(NeevaConstants.apolloURL)
            .okHttpClient(
                OkHttpClient.Builder()
                    .addInterceptor(this)
                    .cookieJar(this)
                    .build()
            )
            .build()
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("User-Agent", "NeevaBrowserAndroid")
            .addHeader("X-Neeva-Client-ID", NeevaConstants.browserIdentifier)
            .addHeader("X-Neeva-Client-Version", BuildConfig.VERSION_NAME)
            .build()
        return chain.proceed(request)
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {}

    @CallSuper
    override fun loadForRequest(url: HttpUrl) =
        mutableListOf(browserTypeCookie, browserVersionCookie)

    @CallSuper
    open suspend fun <D : Query.Data> performQuery(
        query: Query<D>,
        userMustBeLoggedIn: Boolean
    ): ApolloResponse<D>? {
        return try {
            val response = apolloClient.query(query).execute()
            if (response.hasErrors()) {
                Log.e(TAG, "Query response had errors: $query")
                response.errors?.forEach { Log.e(TAG, "\tError: ${it.message}") }
                null
            } else {
                response
            }
        } catch (e: ApolloException) {
            Log.e(TAG, "Could not perform network request", e)
            null
        }
    }

    open fun <D : Mutation.Data> performMutationAsync(
        mutation: Mutation<D>,
        userMustBeLoggedIn: Boolean,
        callback: (ApolloResponse<D>?) -> Unit
    ) {
        coroutineScope.launch(dispatchers.io) {
            val response = performMutation(mutation, userMustBeLoggedIn)
            callback(response)
        }
    }

    @CallSuper
    open suspend fun <D : Mutation.Data> performMutation(
        mutation: Mutation<D>,
        userMustBeLoggedIn: Boolean
    ): ApolloResponse<D>? {
        return try {
            val response = apolloClient.mutation(mutation).execute()
            if (response.hasErrors()) {
                Log.e(TAG, "Mutation response had errors: $mutation")
                response.errors?.forEach { Log.e(TAG, "\tError: ${it.message}") }
                null
            } else {
                response
            }
        } catch (e: ApolloException) {
            Log.e(TAG, "Could not perform network request", e)
            null
        }
    }

    companion object {
        val TAG = ApolloWrapper::class.simpleName
    }
}
