package com.neeva.app.userdata

import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import kotlinx.coroutines.CoroutineScope
import okhttp3.Response
import timber.log.Timber

class IncognitoSessionToken(
    coroutineScope: CoroutineScope,
    dispatchers: Dispatchers,
    neevaConstants: NeevaConstants
) : SessionToken(
    coroutineScope = coroutineScope,
    dispatchers = dispatchers,
    neevaConstants = neevaConstants,
    endpointURL = neevaConstants.incognitoURL,
    cookieName = neevaConstants.incognitoCookieKey
) {
    private var _cachedValue: String = ""
    override val cachedValue: String get() = _cachedValue

    override fun updateCachedCookie(newValue: String) {
        _cachedValue = newValue
    }

    override suspend fun processResponse(response: Response): Boolean {
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
        val result = response.body?.string()?.let { body ->
            createSessionPayloadAdapter.fromJson(body)
        }
        val resultCode = result?.resultCode

        when {
            response.code == 500 || result == null || resultCode == 5 -> {
                Timber.e("Backend error: ${result?.error}")
            }

            response.code == 200 -> {
                Timber.d("Request successful but no cookie was created.")
                return true
            }

            resultCode == 1 && result.sessionKey != null -> {
                Timber.d("New incognito session started.")
                updateCookieManagerAsync(result.sessionKey, result.sessionDuration)
                return true
            }

            else -> {
                Timber.e("Unhandled error occurred: ${response.code}")
            }
        }

        return false
    }
}
