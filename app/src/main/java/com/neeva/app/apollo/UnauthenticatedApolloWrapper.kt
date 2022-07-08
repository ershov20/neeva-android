package com.neeva.app.apollo

import com.neeva.app.NeevaConstants

/** Unauthenticated [BaseApolloWrapper] that doesn't require the user to be logged in. */
open class UnauthenticatedApolloWrapper(
    neevaConstants: NeevaConstants,
    apolloClientWrapper: ApolloClientWrapper = OkHttpApolloClientWrapper(neevaConstants)
) : BaseApolloWrapper(apolloClientWrapper) {
    override fun mayPerformOperation(userMustBeLoggedIn: Boolean): Boolean {
        return !userMustBeLoggedIn
    }
}
