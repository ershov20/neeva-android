package com.neeva.app.apollo

import androidx.annotation.CallSuper
import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.network.okHttpClient
import com.neeva.app.BuildConfig
import com.neeva.app.NeevaConstants
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

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
    val neevaConstants: NeevaConstants,
    val createAdditionalCookies: () -> List<Cookie> = { emptyList() }
) : ApolloClientWrapper, Interceptor, CookieJar {
    private val apolloClient = ApolloClient.Builder()
        .serverUrl(neevaConstants.apolloURL)
        .okHttpClient(
            OkHttpClient.Builder()
                .addInterceptor(this)
                .cookieJar(this)
                .build()
        )
        .build()

    override fun apolloClient() = apolloClient

    /** Appends all the cookies required for the server to act on our requests. */
    @CallSuper
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return mutableListOf<Cookie>().apply {
            add(neevaConstants.browserTypeCookie)
            add(neevaConstants.browserVersionCookie)
            addAll(createAdditionalCookies())
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {}

    /** Appends headers to the request that the server expects to receive. */
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("User-Agent", "NeevaBrowserAndroid")
            .addHeader("X-Neeva-Client-ID", neevaConstants.browserIdentifier)
            .addHeader("X-Neeva-Client-Version", BuildConfig.VERSION_NAME)
            .build()
        return chain.proceed(request)
    }
}
