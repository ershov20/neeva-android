// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.userdata

import android.content.Intent
import android.net.Uri
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import okhttp3.Response
import org.chromium.weblayer.Browser

/** Manages the login cookie that tracks whether the user is signed in or not. */
class LoginToken(
    coroutineScope: CoroutineScope,
    dispatchers: Dispatchers,
    neevaConstants: NeevaConstants,
    private val sharedPreferencesModel: SharedPreferencesModel,
) : SessionToken(
    coroutineScope = coroutineScope,
    dispatchers = dispatchers,
    neevaConstants = neevaConstants,
    endpointURL = null,
    cookieName = neevaConstants.loginCookie
) {
    companion object {
        fun extractAuthTokenFromIntent(intent: Intent?): String? {
            val dataString = intent?.dataString ?: return null
            val dataUri = Uri.parse(dataString)
            if (dataUri.scheme != "neeva" || dataUri.host != "login") {
                return null
            }

            // The URI is not hierarchical because of a backend bug, so none of the nicer
            // getQueryForKey calls work.
            val token = dataUri.query?.substringAfter("sessionKey=")
            return if (token.isNullOrEmpty()) null else token
        }
    }

    override val cookieValue: String
        get() = SharedPrefFolder.User.Token.get(sharedPreferencesModel)
    override val cookieValueFlow: StateFlow<String>
        get() = SharedPrefFolder.User.Token.getFlow(sharedPreferencesModel)

    override fun updateCachedCookie(newValue: String) {
        if (cookieValue == newValue) return
        SharedPrefFolder.User.Token.set(
            sharedPreferencesModel = sharedPreferencesModel,
            value = newValue,
            mustCommitImmediately = true
        )
    }

    override fun purgeCachedCookie() {
        SharedPrefFolder.User.Token.remove(sharedPreferencesModel)
    }

    /** No-op: The user has to provide login credentials via the sign-in flow. */
    override fun requestNewCookie(browser: Browser) {}
    override suspend fun processResponse(response: Response): Boolean = false

    override fun mayPerformOperation(userMustBeLoggedIn: Boolean): Boolean {
        return !userMustBeLoggedIn || isNotEmpty()
    }
}
