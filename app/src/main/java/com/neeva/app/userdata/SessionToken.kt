package com.neeva.app.userdata

import android.net.Uri
import androidx.annotation.MainThread
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaBrowser
import com.neeva.app.NeevaConstants
import com.neeva.app.apollo.NeevaOkHttpClient
import com.neeva.app.browsing.CookiePair
import com.neeva.app.browsing.takeIfAlive
import com.squareup.moshi.Json
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.io.IOException
import java.lang.ref.WeakReference
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
import org.chromium.weblayer.CookieManager
import timber.log.Timber

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
 * Session cookies are synced with the WebLayer [Browser], with the [Browser]'s [CookieManager]
 * being the source of truth for the current cookie.
 */
abstract class SessionToken(
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers,
    private val neevaConstants: NeevaConstants,
    protected val endpointURL: String?,
    val cookieName: String
) {
    /**
     * Cached version of the session token.  The actual source of truth is the WebLayer [Browser]
     * and its [CookieManager], but storing it is helpful for performing network requests.  If you
     * need the most up-to-date version of the token, call [getCookieFromBrowser].
     */
    abstract val cachedValue: String

    /**
     * Weakly held reference to the WebLayer [Browser].
     *
     * The [SessionToken] is a @Singleton at the application level, but the [Browser] is tied to the
     * lifecycle of a Fragment attached to our Activity.  To avoid a memory leak and attempting to
     * use it after it's already been destroyed, we just store a weak reference to it that can be
     * ignored whenever the [Browser] dies.
     */
    protected var weakBrowser: WeakReference<Browser> = WeakReference(null)

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

    fun updateBrowserCookieJarWithCachedCookie(onFailure: () -> Unit) {
        coroutineScope.launch(dispatchers.main) {
            val restoredValue = cachedValue
            if (restoredValue.isNotEmpty()) {
                Timber.d("Cached session cookie detected at startup; re-setting in Browser.")
                // Update the Browser's cookie jar with the login cookie.
                suspendCoroutine { continuation ->
                    // The callback for the update cookie manager function returns false when the
                    // cookie already exists in the browser's cookie jar. This means that if
                    // the cached cookie is equal to the Browser's cookie, the callback would
                    // return false even if the Browser's cookie is up to date.
                    updateCookieManager(restoredValue) {
                        continuation.resume(Unit)
                    }
                }

                // To actually tell if the browser cookie jar is up to date, we have to ask the
                // browser for our cookie:
                val (_, cookiePair) = weakBrowser.getCookieFromBrowser()
                val currentCookieValue = cookiePair?.value ?: ""

                if (currentCookieValue != cachedValue) {
                    onFailure()
                }
            }
        }
    }

    fun initializeCookieManager(browser: Browser, requestCookieIfEmpty: Boolean = false) {
        val cookieManager = browser.takeIfAlive()?.profile?.cookieManager ?: return
        weakBrowser = WeakReference(browser)

        // https://github.com/neevaco/neeva-android/issues/1043
        // In previous versions of the app, login tokens were persisted in SharedPreferences and
        // synced into the Browser's CookieManager at initialization.  While we've since
        // switched to using the Browser's CookieManager as the source of truth, we have to
        // continue the same practice because the old versions of the app incorrectly defined
        // the cookie in the Browser's CookieManager as a temporary session cookie, meaning that
        // it would get wiped when the user restarted the app.
        // To ensure that users don't have to sign in again after installing a newer version of
        // the app, we first check if the cookie has been persisted in memory before we try to
        // sync it back from the browser.
        updateBrowserCookieJarWithCachedCookie(onFailure = {})

        coroutineScope.launch(dispatchers.main) {
            // Detect and save any changes to the session cookie.
            cookieManager.addCookieChangedCallback(
                Uri.parse(neevaConstants.appURL),
                cookieName,
                object : CookieChangedCallback() {
                    override fun onCookieChanged(cookieNameAndValue: String, cause: Int) {
                        // We don't get the latest value when we're told that the cookie changes:
                        // the [cause] hints at what is changing with the [cookieNameAndValue], so
                        // you could be given the old value and told it's being deleted.
                        // Instead, we fetch the current value from the Browser then sync it.
                        syncCachedCookie()
                    }
                }
            )

            // Pull the cookie out of the Browser cookie jar and cache it in memory.
            syncCachedCookie { cookieValue ->
                if (requestCookieIfEmpty && cookieValue.isEmpty()) {
                    requestNewCookie()
                }
            }
        }
    }

    private fun syncCachedCookie(onCookieFetched: (cookieValue: String) -> Unit = {}) {
        // We don't get the latest value when we're told that the cookie has changed
        // so we have to manually get it again.
        coroutineScope.launch(dispatchers.main) {
            val (isBrowserAlive, cookiePair) = weakBrowser.getCookieFromBrowser()
            if (isBrowserAlive) {
                val currentCookieValue = cookiePair?.value ?: ""
                updateCachedCookie(currentCookieValue)
                onCookieFetched(currentCookieValue)
            }
        }
    }

    suspend fun updateCookieManagerAsync(
        newValue: String,
        newDuration: Int? = null,
        callback: (success: Boolean) -> Unit = {}
    ) = withContext(dispatchers.main) {
        updateCookieManager(newValue, newDuration, callback)
    }

    @MainThread
    fun updateCookieManager(
        newValue: String,
        newDuration: Int? = null,
        callback: (success: Boolean) -> Unit = {}
    ) {
        val cookie = when {
            newValue.isEmpty() -> {
                // Delete any existing cookie.
                neevaConstants.createPersistentNeevaCookieString(
                    cookieName = cookieName,
                    cookieValue = "",
                    isSessionToken = true,
                    durationMinutes = 0
                )
            }

            else -> {
                // Set a cookie with the expected value.
                neevaConstants.createPersistentNeevaCookieString(
                    cookieName = cookieName,
                    cookieValue = newValue,
                    isSessionToken = true,
                    durationMinutes = newDuration ?: Int.MAX_VALUE
                )
            }
        }

        val callbackWithLog: (Boolean) -> Unit = { success ->
            if (!success) {
                Timber.e("Failed to set $cookieName in Browser")
            } else {
                Timber.i("Set $cookieName in Browser")
            }

            callback(success)
        }
        weakBrowser.get()?.takeIfAlive()?.profile?.cookieManager?.setCookie(
            Uri.parse(neevaConstants.appURL),
            cookie
        ) { success ->
            callbackWithLog(success)
        } ?: run {
            callbackWithLog(false)
        }
    }

    /** Fires a network request to the backend to ask for a new session cookie. */
    open fun requestNewCookie() {
        if (endpointURL == null || requestJob?.isActive == true) return

        requestJob = coroutineScope.launch(dispatchers.io) {
            val request = Request.Builder()
                .url(endpointURL)
                .post(EMPTY_REQUEST)
                .build()

            try {
                neevaOkHttpClient.client.newCall(request).execute().use { response ->
                    processResponse(response)
                }
            } catch (e: IOException) {
                Timber.e("Failed to request $cookieName session token", e)
            } catch (e: IllegalStateException) {
                Timber.e("Failed to request $cookieName session token", e)
            }
        }
    }

    /**
     * Processes the network response triggered by [requestNewCookie] and saves the result back to
     * WebLayer's [Browser]'s [CookieManager].
     */
    @Throws(IOException::class, IllegalStateException::class)
    protected abstract suspend fun processResponse(response: Response): Boolean

    abstract fun updateCachedCookie(newValue: String)

    open fun purgeCachedCookie(callback: (success: Boolean) -> Unit = {}) {
        updateCachedCookie(newValue = "")
        callback(true)
    }

    fun getSessionCookies(): List<Cookie> {
        return when {
            // We exclude cookies when running instrumentation tests because OkHttp's Cookies
            // require us to set a valid domain, while instrumentation tests operate on 127.0.0.1.
            NeevaBrowser.isBeingInstrumented() -> emptyList()

            // If the token doesn't exist, don't send an empty Cookie.
            cachedValue.isEmpty() -> emptyList()

            else -> {
                listOf(
                    neevaConstants.createPersistentNeevaOkHttpCookie(
                        cookieName = cookieName,
                        cookieValue = cachedValue,
                        isSessionToken = true
                    )
                )
            }
        }
    }

    /**
     * Retrieves the cookie value directly from the WebLayer [Browser] -- if one is available.
     *
     * If no cookie is available (because we have never fetched the cookie, or because the cookie
     * expired), we make one attempt to request the cookie and return that result.
     */
    open suspend fun getOrFetchCookie(): String? {
        var cookieValue = weakBrowser.getCookieFromBrowser().second?.value

        if (cookieValue.isNullOrEmpty()) {
            // Calling requestNewCookie() starts an async job to fetch a new cookie that gets stored
            // inside of the WebLayer Browser's CookieManager.
            requestNewCookie()

            // Wait for the job to complete and then pull it out of the CookeManager.
            requestJob?.join()
            cookieValue = weakBrowser.getCookieFromBrowser().second?.value
        }

        return cookieValue
    }

    protected
    suspend fun WeakReference<Browser>.getCookieFromBrowser(): Pair<Boolean, CookiePair?> {
        return withContext(dispatchers.main) {
            suspendCoroutine { continuation ->
                get()?.takeIfAlive()?.profile?.cookieManager
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

    private fun String.toCookiePair(): CookiePair? {
        val pieces = trim().split('=', limit = 2)
        return if (pieces.size == 2) {
            CookiePair(pieces[0], pieces[1])
        } else {
            null
        }
    }
}
