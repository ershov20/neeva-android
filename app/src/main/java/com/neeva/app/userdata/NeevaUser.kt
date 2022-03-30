package com.neeva.app.userdata

import android.net.Uri
import com.neeva.app.ApolloWrapper
import com.neeva.app.UserInfoQuery
import com.neeva.app.type.SubscriptionType

data class NeevaUserData(
    val id: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val pictureURI: Uri? = null,
    val ssoProvider: NeevaUser.SSOProvider = NeevaUser.SSOProvider.UNKNOWN,
    val subscriptionType: SubscriptionType = SubscriptionType.Unknown
)

class NeevaUser(
    var data: NeevaUserData = NeevaUserData(),
    val neevaUserToken: NeevaUserToken
) {
    private var isLoading: Boolean = false

    fun clearUser() {
        data = NeevaUserData()
    }

    fun isSignedOut(): Boolean {
        return neevaUserToken.getToken().isEmpty() || data.id == null
    }

    suspend fun fetch(apolloWrapper: ApolloWrapper) {
        if (neevaUserToken.getToken().isEmpty()) return

        isLoading = true
        val response = apolloWrapper.performQuery(UserInfoQuery(), userMustBeLoggedIn = false)
        if (response != null) {
            response.data?.user?.let { userQuery ->
                data = NeevaUserData(
                    id = userQuery.id,
                    displayName = userQuery.profile.displayName,
                    email = userQuery.profile.email,
                    pictureURI = Uri.parse(userQuery.profile.pictureURL),
                    ssoProvider = SSOProvider.values()
                        .firstOrNull { it.url == userQuery.authProvider }
                        ?: SSOProvider.UNKNOWN,
                    subscriptionType = SubscriptionType.values()
                        .firstOrNull { it == userQuery.subscriptionType }
                        ?: SubscriptionType.Unknown
                )
            }
        } else {
            clearUser()
        }

        isLoading = false
    }

    enum class SSOProvider(val url: String, val finalPath: String) {
        UNKNOWN("", ""),
        GOOGLE("neeva.co/auth/oauth2/authenticators/google", "/"),
        APPLE("neeva.co/auth/oauth2/authenticators/apple", "/"),
        MICROSOFT("neeva.co/auth/oauth2/authenticators/microsoft", "/"),
        OKTA("neeva.co/auth/oauth2/authenticators/okta", "/?nva")
    }
}
