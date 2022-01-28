package com.neeva.app

import android.content.Context
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.okHttpClient
import com.neeva.app.NeevaConstants.appHost
import com.neeva.app.NeevaConstants.appURL
import com.neeva.app.NeevaConstants.browserTypeCookie
import com.neeva.app.NeevaConstants.browserVersionCookie
import com.neeva.app.NeevaConstants.loginCookie
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

fun createApolloClient(
    @ApplicationContext context: Context,
    neevaUserToken: NeevaUserToken
): ApolloClient {
    return ApolloClient.Builder()
        .serverUrl("${appURL}graphql")
        .okHttpClient(
            OkHttpClient.Builder()
                .addInterceptor(AuthorizationInterceptor(context))
                .cookieJar(AuthCookieJar(neevaUserToken))
                .build()
        )
        .build()
}

private class AuthCookieJar(val neevaUserToken: NeevaUserToken) : CookieJar {
    override fun loadForRequest(url: HttpUrl): MutableList<Cookie> {
        val cookies = mutableListOf(browserTypeCookie, browserVersionCookie)
        val token = neevaUserToken.getToken()
        if (token != null) {
            val authCookie = Cookie.Builder().name(loginCookie).secure()
                .domain(appHost).expiresAt(Long.MAX_VALUE).value(token).build()
            cookies.add(authCookie)
        }
        return cookies
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {}
}

private class AuthorizationInterceptor(val context: Context) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("User-Agent", "NeevaBrowserAndroid")
            .addHeader("X-Neeva-Client-ID", "co.neeva.app.android.browser")
            .addHeader("X-Neeva-Client-Version", "0.0.1")
            .build()
        return chain.proceed(request)
    }
}

fun saveLoginCookieFrom(neevaUserToken: NeevaUserToken, cookie: String) {
    if (cookie.split("=").first() == loginCookie) {
        neevaUserToken.setToken(cookie.split("=").last())
    }
}
