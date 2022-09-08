// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.userdata

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.neeva.app.UserInfoQuery
import com.neeva.app.apollo.ApolloWrapper
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.type.SubscriptionType
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow

@JsonClass(generateAdapter = true)
data class UserInfo(
    val id: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val pictureURL: String? = null,
    val ssoProvider: NeevaUser.SSOProvider = NeevaUser.SSOProvider.UNKNOWN,
    val subscriptionType: SubscriptionType = SubscriptionType.Unknown
)

class NeevaUser(
    val sharedPreferencesModel: SharedPreferencesModel,
    val neevaUserToken: NeevaUserToken
) {
    private val moshiJsonAdapter: JsonAdapter<UserInfo> =
        Moshi.Builder().build().adapter(UserInfo::class.java)

    // TODO(kobec): can use a CompletableDeferred and ask spaces.refresh() to await()
    //  so that it will use a valid userid (instead of null) when fetching spaces.
    //  https://github.com/neevaco/neeva-android/issues/948
    val userInfoFlow: MutableStateFlow<UserInfo?> = MutableStateFlow(null)

    init {
        // Load UserInfo from shared preferences at start-up
        val moshiString: String = SharedPrefFolder.User.UserInfo.get(sharedPreferencesModel)
        val fetchedUserInfo = try {
            moshiJsonAdapter.fromJson(moshiString)
        } catch (e: IOException) {
            null
        }
        userInfoFlow.value = fetchedUserInfo
    }

    fun setUserInfo(newData: UserInfo) {
        userInfoFlow.value = newData

        val newDataAsString = try {
            moshiJsonAdapter.toJson(newData)
        } catch (e: java.lang.AssertionError) {
            ""
        }

        SharedPrefFolder.User.UserInfo.set(
            sharedPreferencesModel = sharedPreferencesModel,
            value = newDataAsString
        )
    }

    fun clearUserInfo() {
        userInfoFlow.value = null
        SharedPrefFolder.User.UserInfo.remove(sharedPreferencesModel)
    }

    fun isSignedOut(): Boolean {
        return neevaUserToken.getToken().isEmpty()
    }

    private fun isConnectedToInternet(context: Context): Boolean {
        val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
        val activeNetwork = connectivityManager?.activeNetwork
        if (connectivityManager == null || activeNetwork == null) {
            return false
        }

        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            ?: false
    }

    /**
     * For an error handling flowchart, see: FetchUserInfoFlowChart.md
     */
    suspend fun fetch(
        apolloWrapper: ApolloWrapper,
        context: Context,
        checkNetworkConnectivityBeforeFetch: Boolean = true
    ) {
        if (neevaUserToken.getToken().isEmpty()) return
        if (checkNetworkConnectivityBeforeFetch && !isConnectedToInternet(context)) return

        val responseSummary = apolloWrapper.performQuery(
            query = UserInfoQuery(),
            userMustBeLoggedIn = false
        )
        val response = responseSummary?.response
        val exception = responseSummary?.exception

        // Clear UserInfo if the exception is not caused by a user's bad network connectivity
        if (
            exception != null &&
            (exception !is ApolloNetworkException && exception !is IllegalStateException)
        ) {
            clearUserInfo()
            Log.e(TAG, "Could not perform UserInfoQuery fetch", exception)
            return
        }

        response?.data?.user?.let { userQuery ->
            if (response.hasErrors()) {
                clearUserInfo()
            } else {
                setUserInfo(
                    UserInfo(
                        id = userQuery.id,
                        displayName = userQuery.profile.displayName,
                        email = userQuery.profile.email,
                        pictureURL = userQuery.profile.pictureURL,
                        ssoProvider = SSOProvider.values()
                            .firstOrNull { it.url == userQuery.authProvider }
                            ?: SSOProvider.UNKNOWN,
                        subscriptionType = SubscriptionType.values()
                            .firstOrNull { it == userQuery.subscriptionType }
                            ?: SubscriptionType.Unknown
                    )
                )
            }
        }
    }

    enum class SSOProvider(val url: String, val finalPath: String) {
        UNKNOWN("", ""),
        GOOGLE("neeva.co/auth/oauth2/authenticators/google", "/"),
        APPLE("neeva.co/auth/oauth2/authenticators/apple", "/"),
        MICROSOFT("neeva.co/auth/oauth2/authenticators/microsoft", "/"),
        OKTA("neeva.co/auth/oauth2/authenticators/okta", "/?nva")
    }

    companion object {
        private const val TAG = "NeevaUser"
    }
}
