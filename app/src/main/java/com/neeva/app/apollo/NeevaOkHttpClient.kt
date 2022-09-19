package com.neeva.app.apollo

import androidx.annotation.CallSuper
import androidx.compose.ui.text.intl.Locale
import com.neeva.app.BuildConfig
import com.neeva.app.NeevaConstants
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response

/** Creates an [OkHttpClient] that adds headers and cookies needed to interact with the website. */
class NeevaOkHttpClient(
    val neevaConstants: NeevaConstants,
    val createAdditionalCookies: () -> List<Cookie> = { emptyList() },
    val onCookiesReceived: (url: HttpUrl, cookies: List<Cookie>) -> Unit = { _, _ -> }
) : Interceptor, CookieJar {
    val client: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(this)
        .cookieJar(this)
        .build()

    /** Appends all the cookies required for the server to act on our requests. */
    @CallSuper
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return mutableListOf<Cookie>().apply {
            add(neevaConstants.browserTypeCookie)
            add(neevaConstants.browserVersionCookie)
            addAll(createAdditionalCookies())
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        onCookiesReceived(url, cookies)
    }

    /** Appends headers to the request that the server expects to receive. */
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("User-Agent", "NeevaBrowserAndroid")
            .addHeader("X-Neeva-Client-ID", neevaConstants.browserIdentifier)
            .addHeader("X-Neeva-Client-Version", BuildConfig.VERSION_NAME)
            .addHeader("accept-language", Locale.current.toLanguageTag())
            .build()
        return chain.proceed(request)
    }
}
