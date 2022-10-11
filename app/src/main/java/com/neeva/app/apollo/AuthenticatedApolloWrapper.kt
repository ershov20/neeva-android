// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.apollo

import com.neeva.app.NeevaConstants
import com.neeva.app.userdata.LoginToken
import com.neeva.app.userdata.PreviewSessionToken
import org.chromium.weblayer.Browser

/** Authenticated [BaseApolloWrapper] that sends the user token if the user is signed in. */
open class AuthenticatedApolloWrapper(
    private val loginToken: LoginToken,
    val previewSessionToken: PreviewSessionToken,
    neevaConstants: NeevaConstants,
    apolloClientWrapper: ApolloClientWrapper = OkHttpApolloClientWrapper(neevaConstants) {
        if (loginToken.isNotEmpty()) {
            loginToken.getSessionCookies()
        } else {
            previewSessionToken.getSessionCookies()
        }
    }
) : BaseApolloWrapper(apolloClientWrapper) {
    override suspend fun prepareForOperation(userMustBeLoggedIn: Boolean): Boolean {
        val previewCookieValue = if (loginToken.isEmpty()) {
            previewSessionToken.getOrFetchCookie()
        } else {
            // The backend gives the login cookie precedence over the preview cookie, ignoring the
            // preview cookie entirely if the login cookie is set.
            null
        }

        return !userMustBeLoggedIn || !loginToken.isEmpty() || !previewCookieValue.isNullOrEmpty()
    }

    fun initializeCookieManager(browser: Browser) {
        // Keep track of the user's login and preview cookies, which we need for various operations.
        loginToken.initializeCookieManager(browser)
        previewSessionToken.initializeCookieManager(browser)
    }
}
