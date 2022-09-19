package com.neeva.app.userdata

import android.net.Uri
import com.neeva.app.NeevaConstants
import com.neeva.app.apollo.NeevaOkHttpClient
import com.neeva.app.browsing.CookiePair
import com.neeva.app.browsing.takeIfAlive
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import okhttp3.Cookie
import org.chromium.weblayer.Browser

/**
 * Payload received when creating new preview or incognito sessions.
 * https://docs.google.com/document/d/1rRStjSXig6HfbaXPl4cXbJH_jGkRpTWdTIGsY7G9Gx0/edit#heading=h.sg0ob8ttl9ru
 */
@JsonClass(generateAdapter = true)
data class CreateSessionPayload(
    @Json(name = "result_code")
    val resultCode: Int?,

    @Json(name = "session_duration")
    val sessionDuration: Int?,

    @Json(name = "session_key")
    val sessionKey: String?,

    @Json(name = "error")
    val error: String?
)

/**
 * Manages various session tokens used by the Neeva backend.
 * See https://docs.google.com/document/d/1rRStjSXig6HfbaXPl4cXbJH_jGkRpTWdTIGsY7G9Gx0/edit#
 */
abstract class SessionToken(private val neevaConstants: NeevaConstants) {
    protected val moshi: Moshi = Moshi.Builder().build()
    protected val createSessionPayloadAdapter: JsonAdapter<CreateSessionPayload> =
        moshi.adapter(CreateSessionPayload::class.java)

    /**
     * OkHttpClient that explicitly doesn't pass any pre-existing session cookies.
     * We don't want to be passing along existing cookies if we're trying to make new ones.
     */
    protected val neevaOkHttpClient = NeevaOkHttpClient(
        neevaConstants = neevaConstants,
        createAdditionalCookies = { getSessionCookies() }
    )

    abstract fun initializeCookieManager(browser: Browser)
    abstract fun updateCookieManager(browser: Browser)

    abstract fun getSessionCookies(): List<Cookie>

    abstract fun mayPerformOperation(userMustBeLoggedIn: Boolean): Boolean

    /** Returns a list of cookies split by key and values. */
    suspend fun Browser.getNeevaCookiePairs(): List<CookiePair> {
        val cookies = suspendCoroutine<List<CookiePair>> { continuation ->
            takeIfAlive()?.profile?.cookieManager?.getCookie(
                Uri.parse(neevaConstants.appURL)
            ) { cookiesString ->
                val cookies = cookiesString
                    .split(";")
                    .mapNotNull { cookie -> cookie.toCookiePair() }
                continuation.resume(cookies)
            } ?: run {
                continuation.resume(emptyList())
            }
        }

        return cookies
    }

    protected fun String.toCookiePair(): CookiePair? {
        val pieces = trim().split('=', limit = 2)
        return if (pieces.size == 2) {
            CookiePair(pieces[0], pieces[1])
        } else {
            null
        }
    }
}
