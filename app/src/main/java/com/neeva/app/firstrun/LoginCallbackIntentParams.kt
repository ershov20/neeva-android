// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.firstrun

import android.content.Intent
import android.net.Uri
import com.neeva.app.R

/**
 * Parameters pulled from the Intent fired by the backend when we have completed a sign in/sign up
 * attempt.
 */
data class LoginCallbackIntentParams(
    /** String corresponding to the user's auth token. Should be set in the browser's cookie jar. */
    val sessionKey: String?,

    /** Pathway of a URL on the neeva.com homepage that the user should be redirected to. */
    val finalPath: String?,

    /** Authentication failed; check neeva/auth/challenge/authserver/login_handlers.go */
    val retryCode: String?
) {
    companion object {
        fun fromLoginCallbackIntent(intent: Intent?): LoginCallbackIntentParams? {
            val dataString = intent?.dataString ?: return null
            val dataUri = Uri.parse(dataString)

            if (dataUri.scheme != "neeva" || dataUri.host != "login" || dataUri.path != "/cb") {
                return null
            }

            // We have to decode the finalPath because it can contain escaped characters.
            return LoginCallbackIntentParams(
                sessionKey = dataUri.getQueryParameter("sessionKey"),
                finalPath = Uri.decode(dataUri.getQueryParameter("finalPath")),
                retryCode = dataUri.getQueryParameter("retry")
            )
        }
    }

    fun getErrorResourceId(): Int {
        return when (retryCode) {
            "NL002", "NL003", "NL004", "NL005", "NL013" -> {
                R.string.used_email_error
            }

            "NL016" -> {
                R.string.invalid_request_error
            }

            else -> {
                // We don't know if the Intent was fired for sign up or sign in,
                // so default to a generic error.
                R.string.error_generic
            }
        }
    }
}
