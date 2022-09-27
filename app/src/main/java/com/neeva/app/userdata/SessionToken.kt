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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.EMPTY_REQUEST
import org.chromium.weblayer.Browser
import org.chromium.weblayer.CookieChangedCallback
import org.chromium.weblayer.CookieManager

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
 * Manages various session cookies used by the Neeva backend.
 * See https://docs.google.com/document/d/1rRStjSXig6HfbaXPl4cXbJH_jGkRpTWdTIGsY7G9Gx0/edit#
 *
 * Session cookies are synced with the WebLayer [Browser]:
 * - Changing the cookie value in the app will update the [Browser] via [updateCookieManager]
 * - Changing the cookie value in the [Browser] will update the app via [CookieChangedCallback]
 */
abstract class SessionToken(
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers,
    private val neevaConstants: NeevaConstants,
    protected val endpointURL: String?,
    protected val cookieName: String
) {
    companion object {
        private const val TAG = "SessionToken"
    }

    abstract val cookieValue: String
    abstract val cookieValueFlow: StateFlow<String>

    fun isEmpty(): Boolean = !isNotEmpty()
    fun isNotEmpty(): Boolean = cookieValue.isNotEmpty()

    protected val createSessionPayloadAdapter: JsonAdapter<CreateSessionPayload> by lazy {
        Moshi.Builder().build().adapter(CreateSessionPayload::class.java)
    }

    private val neevaOkHttpClient by lazy {
        NeevaOkHttpClient(
            neevaConstants = neevaConstants,
            createAdditionalCookies = ::getSessionCookies
        )
    }

    private var requestJob: Job? = null

    open fun initializeCookieManager(browser: Browser) {
        val cookieManager = browser.takeIfAlive()?.profile?.cookieManager ?: return

        // Detect and save any changes to the session cookie.
        cookieManager.addCookieChangedCallback(
            Uri.parse(neevaConstants.appURL),
            cookieName,
            object : CookieChangedCallback() {
                override fun onCookieChanged(cookieNameAndValue: String, cause: Int) {
                    // We don't get the latest value when we're told that the cookie has changed
                    // so we have to manually get it again.
                    coroutineScope.launch {
                        val (isBrowserAlive, cookiePair) = browser.getCurrentCookieValue()
                        if (isBrowserAlive) {
                            cookiePair?.value
                                ?.let { updateCachedCookie(it) }
                                ?: run { purgeCachedCookie() }
                        }
                    }
                }
            }
        )

        // Copy the cookie back from our storage, if we have one set.
        if (cookieValue.isNotEmpty()) {
            updateCookieManager(browser)
        }

        requestNewCookie(browser)
    }

    @MainThread
    fun updateCookieManager(
        cookieManager: CookieManager,
        callback: (success: Boolean) -> Unit = {}
    ) {
        cookieManager.setCookie(
            Uri.parse(neevaConstants.appURL),
            "$cookieName=$cookieValue"
        ) { success ->
            if (!success) Log.e(TAG, "Failed to set $cookieName in Browser")
            callback(success)
        }
    }

    @MainThread
    fun updateCookieManager(browser: Browser, callback: (success: Boolean) -> Unit = {}) {
        browser.takeIfAlive()?.let {
            updateCookieManager(
                cookieManager = it.profile.cookieManager,
                callback = callback
            )
        } ?: run {
            callback(false)
        }
    }

    /**
     * Fires a network request to the backend to ask for a new session cookie, if we don't already
     * have one.
     */
    open fun requestNewCookie(browser: Browser) {
        if (endpointURL == null || requestJob?.isActive == true) return

        // If we've already got a token, don't get a new one.
        if (cookieValue.isNotEmpty()) {
            Log.i(TAG, "Re-using existing $cookieName session token.")
            return
        }

        requestJob = coroutineScope.launch {
            val (isBrowserAlive, existingCookie) = browser.getCurrentCookieValue()

            if (!isBrowserAlive) {
                Log.i(TAG, "Browser is not available; cannot get $cookieName")
                return@launch
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

    abstract fun updateCachedCookie(newValue: String)
    open fun purgeCachedCookie() = updateCachedCookie("")

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
    private suspend fun Browser.getCurrentCookieValue(): Pair<Boolean, CookiePair?> {
        return withContext(dispatchers.main) {
            suspendCoroutine { continuation ->
                takeIfAlive()?.profile?.cookieManager
                    ?.getCookie(Uri.parse(neevaConstants.appURL)) { cookiesString ->
                        // WebLayer returns all cookies joined together by `;` characters.
                        val cookie = cookiesString
                            .split(";")
                            .mapNotNull { cookie -> cookie.toCookiePair() }
                            .firstOrNull { it.key == cookieName }
                        continuation.resume(Pair(true, cookie))
                    }
                    ?: run {
                        // The browser isn't available.
                        continuation.resume(Pair(false, null))
                    }
            }
        }
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
