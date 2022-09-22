package com.neeva.app.userdata

import android.net.Uri
import android.util.Log
import androidx.annotation.MainThread
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.apollo.NeevaOkHttpClient
import com.neeva.app.browsing.CookiePair
import com.neeva.app.browsing.takeIfAlive
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.EMPTY_REQUEST
import org.chromium.weblayer.Browser
import org.chromium.weblayer.CookieChangedCallback

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
abstract class SessionToken(
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers,
    private val neevaConstants: NeevaConstants,
    protected val endpointURL: String,
    protected val cookieName: String
) {
    companion object {
        private const val TAG = "SessionToken"
    }

    protected abstract val cookieValue: String

    protected val createSessionPayloadAdapter: JsonAdapter<CreateSessionPayload> =
        Moshi.Builder().build().adapter(CreateSessionPayload::class.java)

    private val neevaOkHttpClient = NeevaOkHttpClient(
        neevaConstants = neevaConstants,
        createAdditionalCookies = ::getSessionCookies
    )

    private var requestJob: Job? = null

    open fun initializeCookieManager(browser: Browser) {
        browser.takeIfAlive()?.profile?.cookieManager?.apply {
            // Detect and save any changes to the session cookie.
            addCookieChangedCallback(
                Uri.parse(neevaConstants.appURL),
                cookieName,
                object : CookieChangedCallback() {
                    override fun onCookieChanged(cookieNameAndValue: String, cause: Int) {
                        cookieNameAndValue.toCookiePair()
                            ?.takeIf { it.key == cookieName }
                            ?.let { updateCachedCookie(it.value) }
                    }
                }
            )

            // Copy the cookie back from our storage, if we have one set.
            if (cookieValue.isNotEmpty()) {
                updateCookieManager(browser)
            }

            requestNewCookie(browser)
        }
    }

    @MainThread
    fun updateCookieManager(browser: Browser) {
        browser.takeIfAlive()?.profile?.cookieManager?.setCookie(
            Uri.parse(neevaConstants.appURL),
            "$cookieName=$cookieValue"
        ) { success ->
            if (!success) Log.e(TAG, "Failed to set $cookieName in Browser")
        }
    }

    fun requestNewCookie(browser: Browser) {
        if (requestJob?.isActive == true) return

        // If we've already got a token, don't get a new one.
        if (cookieValue.isNotEmpty()) {
            Log.i(TAG, "Re-using existing $cookieName session token.")
            return
        }

        requestJob = coroutineScope.launch {
            val existingCookie = withContext(dispatchers.main) {
                val cookies = browser.getNeevaCookiePairs()
                cookies
                    .firstOrNull { it.key == cookieName }
                    ?.value
            }

            if (existingCookie != null) {
                Log.i(TAG, "Re-using existing $cookieName session token.")
                return@launch
            }

            withContext(dispatchers.io) {
                val request = Request.Builder()
                    .url(endpointURL)
                    .post(EMPTY_REQUEST)
                    .build()

                try {
                    neevaOkHttpClient.client.newCall(request).execute().use { response ->
                        if (processResponse(response)) {
                            withContext(dispatchers.main) {
                                updateCookieManager(browser)
                            }
                        }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to request $cookieName session token", e)
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Failed to request $cookieName session token", e)
                }
            }
        }
    }

    @Throws(IOException::class, IllegalStateException::class)
    abstract suspend fun processResponse(response: Response): Boolean

    protected abstract fun updateCachedCookie(cookieValue: String)

    fun getSessionCookies(): List<Cookie> {
        return if (cookieValue.isNotEmpty()) {
            listOf(
                neevaConstants.createNeevaCookie(
                    cookieName = cookieName,
                    cookieValue = cookieValue
                )
            )
        } else {
            emptyList()
        }
    }

    abstract fun mayPerformOperation(userMustBeLoggedIn: Boolean): Boolean

    /** Returns a list of cookies split by key and values. */
    private suspend fun Browser.getNeevaCookiePairs(): List<CookiePair> {
        val cookies = suspendCoroutine<List<CookiePair>> { continuation ->
            takeIfAlive()?.profile?.cookieManager?.getCookie(
                Uri.parse(neevaConstants.appURL)
            ) { cookiesString ->
                // WebLayer returns all cookies joined together by `;` characters.
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
