package com.neeva.app.userdata

import android.util.Log
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.Response

class IncognitoSessionToken(
    coroutineScope: CoroutineScope,
    dispatchers: Dispatchers,
    neevaConstants: NeevaConstants
) : SessionToken(
    coroutineScope = coroutineScope,
    dispatchers = dispatchers,
    neevaConstants = neevaConstants,
    endpointURL = neevaConstants.incognitoURL,
    cookieName = neevaConstants.incognitoCookie
) {
    companion object {
        private const val TAG = "IncognitoSessionToken"
    }

    override val cookieValueFlow = MutableStateFlow("")
    override val cookieValue: String get() = cookieValueFlow.value

    override fun updateCachedCookie(newValue: String) {
        cookieValueFlow.value = newValue
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
                Log.e(TAG, "Backend error: ${result?.error}")
            }

            response.code == 200 -> {
                Log.d(TAG, "Request successful but no cookie was created.")
                return true
            }

            resultCode == 1 && result.sessionKey != null -> {
                Log.d(TAG, "New incognito session started.")
                updateCachedCookie(result.sessionKey)
                return true
            }

            else -> {
                Log.e(TAG, "Unhandled error occurred: ${response.code}")
            }
        }

        return false
    }

    override fun mayPerformOperation(userMustBeLoggedIn: Boolean): Boolean {
        return !userMustBeLoggedIn
    }
}
