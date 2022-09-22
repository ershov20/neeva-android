package com.neeva.app.userdata

import android.util.Log
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import kotlinx.coroutines.CoroutineScope
import okhttp3.Response

class PreviewSessionToken(
    coroutineScope: CoroutineScope,
    dispatchers: Dispatchers,
    neevaConstants: NeevaConstants,
    private val sharedPreferencesModel: SharedPreferencesModel
) : SessionToken(
    coroutineScope = coroutineScope,
    dispatchers = dispatchers,
    neevaConstants = neevaConstants,
    endpointURL = neevaConstants.previewCookieURL,
    cookieName = neevaConstants.previewCookie
) {
    companion object {
        private const val TAG = "PreviewSessionToken"
    }

    override val cookieValue: String
        get() = SharedPrefFolder.User.PreviewToken.get(sharedPreferencesModel)

    override fun updateCachedCookie(cookieValue: String) {
        SharedPrefFolder.User.PreviewToken.set(
            sharedPreferencesModel = sharedPreferencesModel,
            value = cookieValue
        )
    }

    override suspend fun processResponse(response: Response): Boolean {
        /* Response codes:
         *   201 indicates a new session was created
         *   200 indicates no session was created, but the request was processed correctly
         *   500 indicates an error was encountered while processing the request
         *
         * Result codes:
         *   0 = unknown issue encountered
         *   1 = new preview session created
         *   2 = no session created; the request was already authenticated with an existing
         *       preview session (httpd~preview)
         *   3 = no session created; request was already authenticated with an existing
         *       incognito session (httpd~incognito)
         *   4 = no session created; request was already authenticated with an existing
         *       login session (httpd~login)
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

            resultCode == 2 || resultCode == 3 || resultCode == 4 -> {
                Log.d(TAG, "No cookie was created: $resultCode")
            }

            response.code == 200 -> {
                Log.d(TAG, "Request successful but no cookie was created: $resultCode")
                return true
            }

            result.resultCode == 1 && result.sessionKey != null -> {
                Log.d(TAG, "New $cookieName session started.")
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
        return !userMustBeLoggedIn || cookieValue.isNotEmpty()
    }
}
