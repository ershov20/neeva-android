package com.neeva.app

import android.content.Context
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.network.okHttpClient
import com.neeva.app.NeevaConstants.appHost
import com.neeva.app.NeevaConstants.appURL
import com.neeva.app.NeevaConstants.loginCookie
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.*

fun createApolloClient(@ApplicationContext context: Context): ApolloClient {
    return ApolloClient.Builder()
        .serverUrl("${appURL}graphql")
        .okHttpClient(
            OkHttpClient.Builder()
                .addInterceptor(AuthorizationInterceptor(context))
                .cookieJar(AuthCookieJar(context))
                .build()
        )
        .build()
}

private class AuthCookieJar(val context: Context): CookieJar {
    override fun loadForRequest(url: HttpUrl): MutableList<Cookie> {
        val browserTypeCookie = Cookie.Builder().name("BrowserType").secure()
            .domain(appHost).expiresAt(Long.MAX_VALUE).value("neeva-android").build()
        val browserVersionCookie = Cookie.Builder().name("BrowserVersion").secure()
            .domain(appHost).expiresAt(Long.MAX_VALUE).value("0.0.1").build()
        val cookies = mutableListOf(browserTypeCookie, browserVersionCookie)
        val token = User.getToken(context)
        if (token != null) {
            val authCookie = Cookie.Builder().name(loginCookie).secure()
                .domain(appHost).expiresAt(Long.MAX_VALUE).value(token).build()
            cookies.add(authCookie)
        }
        return cookies
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {}
}

private class AuthorizationInterceptor(val context: Context): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("User-Agent", "NeevaBrowserAndroid")
            .addHeader("X-Neeva-Client-ID", "co.neeva.app.android.browser")
            .addHeader("X-Neeva-Client-Version",  "0.0.1")
            .build()
        return chain.proceed(request)
    }
}

fun saveLoginCookieFrom(context: Context, cookie: String) {
    if (cookie.split("=").first() == loginCookie) {
        User.setToken(context, cookie.split("=").last())
    }
}
