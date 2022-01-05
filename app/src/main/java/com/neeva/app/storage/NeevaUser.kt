package com.neeva.app.storage

import android.net.Uri
import com.apollographql.apollo3.ApolloClient
import com.neeva.app.UserInfoQuery

data class NeevaUser (
    val id: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val pictureUrl: Uri? = null,
    val ssoProvider: SSOProvider = SSOProvider.UNKNOWN
) {
    private var isLoading: Boolean = false

    companion object {
        var shared = NeevaUser()

        suspend fun fetch(apolloClient: ApolloClient) {
            shared.isLoading = true
            val response = apolloClient
                .query(UserInfoQuery())
                .execute()

            response.data?.user?.let { userQuery ->
                shared = NeevaUser(
                    id = userQuery.id,
                    displayName = userQuery.profile.displayName,
                    email = userQuery.profile.email,
                    pictureUrl = Uri.parse(userQuery.profile.pictureURL),
                    ssoProvider = SSOProvider.values()
                        .firstOrNull { it.url == userQuery.authProvider }
                        ?: SSOProvider.UNKNOWN
                )
                shared.isLoading = false
            }
        }
    }
}

enum class SSOProvider(val url: String) {
    UNKNOWN(""),
    GOOGLE("neeva.co/auth/oauth2/authenticators/google"),
    APPLE("neeva.co/auth/oauth2/authenticators/apple"),
    MICROSOFT("neeva.co/auth/oauth2/authenticators/microsoft"),
    OKTA("neeva.co/auth/oauth2/authenticators/okta")
}
