// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.apollo

import com.neeva.app.NeevaConstants
import com.neeva.app.userdata.LoginToken

/** Authenticated [BaseApolloWrapper] that sends the user token if the user is signed in. */
open class AuthenticatedApolloWrapper(
    private val loginToken: LoginToken,
    neevaConstants: NeevaConstants,
    apolloClientWrapper: ApolloClientWrapper = OkHttpApolloClientWrapper(neevaConstants) {
        loginToken.getSessionCookies()
    }
) : BaseApolloWrapper(apolloClientWrapper) {
    override suspend fun prepareForOperation(userMustBeLoggedIn: Boolean): Boolean {
        return !userMustBeLoggedIn || !loginToken.isEmpty()
    }
}
