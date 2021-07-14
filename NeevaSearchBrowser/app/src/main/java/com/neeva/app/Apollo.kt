package com.neeva.app

import android.content.Context
import android.os.Looper
import com.apollographql.apollo.ApolloClient
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

var appHost: String = "neeva.com"

private var instance: ApolloClient? = null

fun apolloClient(context: Context): ApolloClient {
    check(Looper.myLooper() == Looper.getMainLooper()) {
        "Only the main thread can get the apolloClient instance"
    }

    if (instance != null) {
        return instance!!
    }

    instance = ApolloClient.builder()
        .serverUrl("https://${appHost}/graphql")
        .okHttpClient(
            OkHttpClient.Builder()
                .addInterceptor(AuthorizationInterceptor(context))
                .build()
        )
        .build()

    return instance!!
}

private class AuthorizationInterceptor(val context: Context): Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("Authorization", User.getToken(context) ?: "")
            .addHeader("User-Agent", "NeevaBrowserAndroid")
            .addHeader("X-Neeva-Client-ID", "com.neeva.app.android.browser")
            .addHeader("X-Neeva-Client-Version",  "0.0.1")
            .build()

        return chain.proceed(request)
    }
}