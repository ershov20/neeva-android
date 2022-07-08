package com.neeva.app.apollo

import com.neeva.app.NeevaConstants
import com.neeva.app.userdata.NeevaUserToken
import okhttp3.Cookie

/** Authenticated [BaseApolloWrapper] that sends the user token if the user is signed in. */
open class AuthenticatedApolloWrapper(
    private val neevaUserToken: NeevaUserToken,
    neevaConstants: NeevaConstants,
    apolloClientWrapper: ApolloClientWrapper = OkHttpApolloClientWrapper(neevaConstants) {
        createLoginCookie(neevaUserToken, neevaConstants)
    }
) : BaseApolloWrapper(apolloClientWrapper) {
    override fun mayPerformOperation(userMustBeLoggedIn: Boolean): Boolean {
        return !userMustBeLoggedIn || neevaUserToken.getToken().isNotEmpty()
    }

    companion object {
        fun createLoginCookie(
            neevaUserToken: NeevaUserToken,
            neevaConstants: NeevaConstants
        ): List<Cookie> {
            val token = neevaUserToken.getToken()
            return if (token.isNotEmpty()) {
                listOf(
                    Cookie.Builder()
                        .name(neevaConstants.loginCookie)
                        .secure()
                        .domain(neevaConstants.appHost)
                        .expiresAt(Long.MAX_VALUE)
                        .value(token)
                        .build()
                )
            } else {
                emptyList()
            }
        }
    }
}
