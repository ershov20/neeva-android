package com.neeva.app.userdata

import android.net.Uri
import android.util.Log
import androidx.annotation.MainThread
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.browsing.takeIfAlive
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.Request
import okhttp3.internal.EMPTY_REQUEST
import org.chromium.weblayer.Browser
import org.chromium.weblayer.CookieChangedCallback

class IncognitoSessionToken(
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers,
    private val neevaConstants: NeevaConstants
) : SessionToken(neevaConstants = neevaConstants) {
    companion object {
        private const val TAG = "IncognitoSessionToken"
    }

    private val _incognitoCookieFlow = MutableStateFlow("")
    val incognitoCookieFlow: StateFlow<String> get() = _incognitoCookieFlow

    private var incognitoRequestJob: Job? = null

    fun updateCachedIncognitoCookie(cookie: String) {
        _incognitoCookieFlow.value = cookie
    }

    fun clearIncognitoCookie() = updateCachedIncognitoCookie("")

    override fun initializeCookieManager(browser: Browser) {
        browser.takeIfAlive()?.profile?.cookieManager?.apply {
            // Detect and save any changes to the incognito cookie.
            addCookieChangedCallback(
                Uri.parse(neevaConstants.appURL),
                neevaConstants.incognitoCookie,
                object : CookieChangedCallback() {
                    override fun onCookieChanged(cookieNameAndValue: String, cause: Int) {
                        cookieNameAndValue.toCookiePair()
                            ?.takeIf { it.key == neevaConstants.incognitoCookie }
                            ?.let { updateCachedIncognitoCookie(it.value) }
                    }
                }
            )

            // Copy the cookie back from our storage, if we have one set.
            if (_incognitoCookieFlow.value.isNotEmpty()) {
                updateCookieManager(browser)
            }

            requestIncognitoCookie(browser)
        }
    }

    private fun requestIncognitoCookie(browser: Browser) {
        if (incognitoRequestJob?.isActive == true) return

        // If we've already got a token, don't get a new one.
        if (_incognitoCookieFlow.value.isNotEmpty()) {
            Log.i(TAG, "Re-using existing incognito session token.")
            return
        }

        incognitoRequestJob = coroutineScope.launch {
            val existingCookie = withContext(dispatchers.main) {
                val cookies = browser.getNeevaCookiePairs()
                cookies
                    .firstOrNull { it.key == neevaConstants.incognitoCookie }
                    ?.value
            }

            if (existingCookie != null) {
                Log.i(TAG, "Re-using existing incognito session cookie")
                return@launch
            }

            withContext(dispatchers.io) {
                /* Response codes:
                 *   201 indicates a new session was created
                 *   200 indicates no session was created, but the request was processed correctly
                 *   500 indicates an error was encountered while processing the request
                 *
                 * Result codes:
                 *   0 = unknown issue encountered
                 *   1 = new incognito session created
                 *   5 = no session was created; something went wrong (see error field)
                 */
                val request = Request.Builder()
                    .url(neevaConstants.incognitoURL)
                    .post(EMPTY_REQUEST)
                    .build()

                try {
                    neevaOkHttpClient.client.newCall(request).execute().use { response ->
                        val result = response.body?.string()?.let { body ->
                            createSessionPayloadAdapter.fromJson(body)
                        }

                        when {
                            response.code == 500 || result == null || result.resultCode == 5 -> {
                                Log.e(TAG, "Backend error: ${result?.error}")
                            }

                            response.code == 200 -> {
                                Log.d(TAG, "Request successful but no cookie was created.")
                                withContext(dispatchers.main) { updateCookieManager(browser) }
                            }

                            result.resultCode == 1 && result.sessionKey != null -> {
                                Log.d(TAG, "New incognito session started.")
                                updateCachedIncognitoCookie(result.sessionKey)
                                withContext(dispatchers.main) { updateCookieManager(browser) }
                            }

                            else -> {
                                Log.e(TAG, "Unhandled error occurred: ${response.code}")
                            }
                        }
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Failed to request incognito token", e)
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Failed to request incognito token", e)
                }
            }
        }
    }

    @MainThread
    override fun updateCookieManager(browser: Browser) {
        browser.takeIfAlive()?.profile?.cookieManager?.setCookie(
            Uri.parse(neevaConstants.appURL),
            "${neevaConstants.incognitoCookie}=${_incognitoCookieFlow.value}"
        ) { success ->
            if (!success) Log.e(TAG, "Failed to set incognito cookie in Browser")
        }
    }

    override fun mayPerformOperation(userMustBeLoggedIn: Boolean): Boolean {
        return !userMustBeLoggedIn
    }

    override fun getSessionCookies(): List<Cookie> {
        val incognitoCookie = _incognitoCookieFlow.value
        return if (incognitoCookie.isNotEmpty()) {
            listOf(
                neevaConstants.createNeevaCookie(
                    cookieName = neevaConstants.incognitoCookie,
                    cookieValue = incognitoCookie
                )
            )
        } else {
            emptyList()
        }
    }
}
