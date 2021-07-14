package com.neeva.app

import android.content.Context
import android.os.Looper
import android.webkit.CookieManager
import com.apollographql.apollo.ApolloClient
import okhttp3.*

var appHost: String = "neeva.com"
var appUrl: String = "https://${appHost}/"

private var instance: ApolloClient? = null

fun apolloClient(context: Context): ApolloClient {
    check(Looper.myLooper() == Looper.getMainLooper()) {
        "Only the main thread can get the apolloClient instance"
    }

    if (instance != null) {
        return instance!!
    }

    instance = ApolloClient.builder()
        .serverUrl("${appUrl}graphql")
        .okHttpClient(
            OkHttpClient.Builder()
                .addInterceptor(AuthorizationInterceptor(context))
                .cookieJar(AuthCookieJar(context))
                .build()
        )
        .build()

    return instance!!
}

private class AuthCookieJar(val context: Context): CookieJar {
    override fun loadForRequest(url: HttpUrl): MutableList<Cookie> {
        val authCookie = Cookie.Builder().name("httpd~login").secure()
            .domain(appHost).expiresAt(Long.MAX_VALUE).value(User.getToken(context = context)).build()
        return mutableListOf(authCookie)
    }

    override fun saveFromResponse(url: HttpUrl, cookies: MutableList<Cookie>) {}
}

private class AuthorizationInterceptor(val context: Context): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("User-Agent", "NeevaBrowserAndroid")
            .addHeader("X-Neeva-Client-ID", "com.neeva.app.android.browser")
            .addHeader("X-Neeva-Client-Version",  "0.0.1")
            .build()
        return chain.proceed(request)
    }
}