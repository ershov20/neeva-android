// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.apollo

import com.neeva.app.NeevaConstants
import com.neeva.app.userdata.IncognitoSessionToken

/** [ApolloWrapper] that uses Incognito tokens to keep them logged out and anonymous. */
class IncognitoApolloWrapper(
    private val incognitoSessionToken: IncognitoSessionToken,
    neevaConstants: NeevaConstants,
    apolloClientWrapper: ApolloClientWrapper = OkHttpApolloClientWrapper(neevaConstants) {
        incognitoSessionToken.getSessionCookies()
    }
) : BaseApolloWrapper(apolloClientWrapper) {
    override suspend fun prepareForOperation(userMustBeLoggedIn: Boolean): Boolean {
        var cookieValue = incognitoSessionToken.getCurrentCookieValue()
        if (cookieValue.isNullOrEmpty()) {
            incognitoSessionToken.requestNewCookie()
            cookieValue = incognitoSessionToken.waitForRequest()
        }

        return !userMustBeLoggedIn || !cookieValue.isNullOrEmpty()
    }
}
