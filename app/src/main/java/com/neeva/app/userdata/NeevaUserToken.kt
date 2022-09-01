// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.userdata

import android.content.Intent
import android.net.Uri
import com.neeva.app.NeevaConstants
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel

/**
 * Singleton that provides and saves Neeva user identity token to SharedPrefs.
 */
class NeevaUserToken(
    private val sharedPreferencesModel: SharedPreferencesModel,
    private val neevaConstants: NeevaConstants
) {
    companion object {
        fun extractAuthTokenFromIntent(intent: Intent?): String? {
            val dataString = intent?.dataString ?: return null
            val dataUri = Uri.parse(dataString)
            if (dataUri.scheme != "neeva" || dataUri.host != "login") {
                return null
            }

            // The URI is not hierarchical so none of the nicer getQueryForKey calls work.
            val token = dataUri.query?.substringAfter("sessionKey=")
            return if (token.isNullOrEmpty()) null else token
        }
    }

    fun loginCookieString(): String {
        return "${neevaConstants.loginCookie}=${getToken()}"
    }

    fun getToken(): String {
        return SharedPrefFolder.User.Token.get(sharedPreferencesModel)
    }

    fun setToken(token: String) {
        SharedPrefFolder.User.Token.set(
            sharedPreferencesModel = sharedPreferencesModel,
            value = token,
            mustCommitImmediately = true
        )
    }

    fun removeToken() {
        SharedPrefFolder.User.Token.remove(sharedPreferencesModel)
    }
}
