package com.neeva.app.storage

import android.net.Uri
import android.util.Log
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.neeva.app.NeevaUserToken
import com.neeva.app.UserInfoQuery
import com.neeva.app.browsing.WebLayerModel

data class NeevaUserData(
    val id: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val pictureUrl: Uri? = null,
    val ssoProvider: NeevaUser.SSOProvider = NeevaUser.SSOProvider.UNKNOWN,
)

class NeevaUser(
    var data: NeevaUserData = NeevaUserData(),
    val neevaUserToken: NeevaUserToken
) {
    private val TAG = "NeevaUser"
    private var isLoading: Boolean = false

    fun clearUser() {
        data = NeevaUserData()
    }

    fun signOut(webLayerModel: WebLayerModel? = null) {
        clearUser()
        webLayerModel?.clearNeevaCookies()
        webLayerModel?.currentBrowser?.activeTabModel?.reload()
    }

    fun isSignedOut(): Boolean {
        return neevaUserToken.getToken().isEmpty() || data.id == null
    }

    suspend fun fetch(apolloClient: ApolloClient) {
        if (neevaUserToken.getToken().isNotEmpty()) {
            isLoading = true
            try {
                val response = apolloClient
                    .query(UserInfoQuery())
                    .execute()

                response.data?.user?.let { userQuery ->
                    data = NeevaUserData(
                        id = userQuery.id,
                        displayName = userQuery.profile.displayName,
                        email = userQuery.profile.email,
                        pictureUrl = Uri.parse(userQuery.profile.pictureURL),
                        ssoProvider = SSOProvider.values()
                            .firstOrNull { it.url == userQuery.authProvider }
                            ?: SSOProvider.UNKNOWN
                    )
                }
            } catch (e: ApolloNetworkException) {
                Log.e(TAG, "Failed to perform request", e)
                clearUser()
            }
            isLoading = false
        }
    }

    enum class SSOProvider(val url: String, val finalPath: String) {
        UNKNOWN("", ""),
        GOOGLE("neeva.co/auth/oauth2/authenticators/google", "/"),
        APPLE("neeva.co/auth/oauth2/authenticators/apple", "/"),
        MICROSOFT("neeva.co/auth/oauth2/authenticators/microsoft", "/"),
        OKTA("neeva.co/auth/oauth2/authenticators/okta", "/?nva")
    }
}
