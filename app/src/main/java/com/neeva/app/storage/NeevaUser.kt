package com.neeva.app.storage

import android.net.Uri
import com.apollographql.apollo.coroutines.await
import com.neeva.app.ListSpacesQuery
import com.neeva.app.NeevaBrowser
import com.neeva.app.UserInfoQuery
import com.neeva.app.apolloClient

data class NeevaUserInfo (
    val id: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val pictureUrl: Uri? = null,
    val ssoProvider: SSOProvider = SSOProvider.UNKNOWN
) {
    private var isLoading: Boolean = false

    companion object {
        var shared = NeevaUserInfo()

        suspend fun fetch() {
            shared.isLoading = true
            val response = apolloClient(NeevaBrowser.context).query(
                UserInfoQuery()
            ).await()

            response.data?.user?.let { userQuery ->
                shared = NeevaUserInfo(
                    id = userQuery.id,
                    displayName = userQuery.profile.displayName,
                    email = userQuery.profile.email,
                    pictureUrl = Uri.parse(userQuery.profile.pictureURL),
                    ssoProvider = SSOProvider.values().firstOrNull {
                        it.url == userQuery.authProvider
                    } ?: SSOProvider.UNKNOWN
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