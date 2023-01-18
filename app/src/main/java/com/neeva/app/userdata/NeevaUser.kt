// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.userdata

import android.content.Context
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.neeva.app.Dispatchers
import com.neeva.app.UserInfoQuery
import com.neeva.app.apollo.ApolloWrapper
import com.neeva.app.billing.billingclient.BillingClientController
import com.neeva.app.network.NetworkHandler
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.type.SubscriptionSource
import com.neeva.app.type.SubscriptionType
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import timber.log.Timber

@JsonClass(generateAdapter = true)
data class UserInfo(
    val id: String? = null,
    val displayName: String? = null,
    val email: String? = null,
    val pictureURL: String? = null,
    val ssoProviderString: String = NeevaUser.SSOProvider.UNKNOWN.name,
    val subscriptionTypeString: String = SubscriptionType.Unknown.name,
    val subscriptionSourceString: String = SubscriptionSource.UNKNOWN__.name
) {
    val ssoProvider: NeevaUser.SSOProvider get() =
        NeevaUser.SSOProvider.values().firstOrNull { it.name == ssoProviderString }
            ?: NeevaUser.SSOProvider.UNKNOWN

    // Normally would just have to annotate enum class with: @JsonClass(generateAdapter=false)
    // but since SubscriptionType is generated, we have to store a string instead.
    val subscriptionType: SubscriptionType get() =
        SubscriptionType.values().firstOrNull { it.name == subscriptionTypeString }
            ?: SubscriptionType.Unknown

    val subscriptionSource: SubscriptionSource get() =
        SubscriptionSource.values().firstOrNull { it.name == subscriptionSourceString }
            ?: SubscriptionSource.UNKNOWN__
}

abstract class NeevaUser(val loginToken: LoginToken) {
    enum class SSOProvider(val url: String, val finalPath: String) {
        UNKNOWN("", ""),
        GOOGLE("neeva.co/auth/oauth2/authenticators/google", "/"),
        APPLE("neeva.co/auth/oauth2/authenticators/apple", "/"),
        MICROSOFT("neeva.co/auth/oauth2/authenticators/microsoft", "/"),
        OKTA("neeva.co/auth/oauth2/authenticators/okta", "/")
    }

    // TODO(kobec): can use a CompletableDeferred and ask spaces.refresh() to await()
    //  so that it will use a valid userid (instead of null) when fetching spaces.
    //  https://github.com/neevaco/neeva-android/issues/948
    val userInfoFlow: MutableStateFlow<UserInfo?> = MutableStateFlow(null)

    abstract fun setUserInfo(newData: UserInfo)
    abstract fun clearUserInfo()
    abstract fun isSignedOut(): Boolean
    abstract suspend fun fetch(
        apolloWrapper: ApolloWrapper,
        context: Context,
        ignoreLastFetchTimestamp: Boolean = false
    )
    /**
     * Queues a [job] to run when a user signs in successfully.
     * If the [uniqueJobName] already exists in the queue, it will be replaced by the new [job].
     * This [job] will only run once.
     */
    abstract fun queueOnSignIn(uniqueJobName: String, job: suspend () -> Unit)
}

class NeevaUserImpl(
    val coroutineScope: CoroutineScope,
    val dispatchers: Dispatchers,
    val sharedPreferencesModel: SharedPreferencesModel,
    val networkHandler: NetworkHandler,
    loginToken: LoginToken,
    private val billingClientController: BillingClientController
) : NeevaUser(loginToken) {
    private data class OnSignedInJob(
        val uniqueJobName: String,
        val job: suspend () -> Unit
    )

    private val moshiJsonAdapter: JsonAdapter<UserInfo> =
        Moshi.Builder().build().adapter(UserInfo::class.java)
    private val jobsToRunFlow: MutableStateFlow<Set<OnSignedInJob>> =
        MutableStateFlow(mutableSetOf())

    /** When the last [fetch] was performed. */
    private var lastFetchTimestamp: Long = 0

    init {
        // Load UserInfo from shared preferences at start-up
        val moshiString: String = SharedPrefFolder.User.UserInfo.get(sharedPreferencesModel)
        val fetchedUserInfo = try {
            moshiJsonAdapter.fromJson(moshiString)
        } catch (e: IOException) {
            null
        }
        userInfoFlow.value = fetchedUserInfo
        startSignInJobConsumer()
        queueOnSignIn(
            uniqueJobName = "Fetch ObfuscatedUserID",
            job = {
                billingClientController.onUserSignedIn()
            }
        )
    }

    private fun startSignInJobConsumer() {
        coroutineScope.launch(dispatchers.main) {
            jobsToRunFlow
                .combine(userInfoFlow) { jobSet, userInfo ->
                    if (userInfo == null || jobSet.isEmpty()) {
                        null
                    } else {
                        jobSet
                    }
                }
                .filterNotNull()
                .collect { jobSet ->
                    val jobToRun = jobSet.first()
                    jobToRun.job.invoke()
                    jobsToRunFlow.value = jobsToRunFlow.value.minus(jobToRun)
                }
        }
    }

    override fun setUserInfo(newData: UserInfo) {
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

    override fun clearUserInfo() {
        userInfoFlow.value = null
        SharedPrefFolder.User.UserInfo.remove(sharedPreferencesModel)
    }

    override fun isSignedOut(): Boolean {
        return loginToken.isEmpty()
    }

    override fun queueOnSignIn(uniqueJobName: String, job: suspend () -> Unit) {
        coroutineScope.launch(dispatchers.main) {
            val currentJobs = jobsToRunFlow.value.toMutableSet()
            currentJobs.removeIf { it.uniqueJobName == uniqueJobName }
            currentJobs.add(OnSignedInJob(uniqueJobName = uniqueJobName, job = job))
            jobsToRunFlow.value = currentJobs
        }
    }

    /**
     * For an error handling flowchart, see: FetchUserInfoFlowChart.md
     */
    override suspend fun fetch(
        apolloWrapper: ApolloWrapper,
        context: Context,
        ignoreLastFetchTimestamp: Boolean
    ) {
        if (loginToken.isEmpty()) return
        if (!networkHandler.isConnectedToInternet()) return

        // Don't perform a fetch if one was performed recently.
        val currentTimestamp = System.currentTimeMillis()
        val elapsed = currentTimestamp - lastFetchTimestamp
        if (!ignoreLastFetchTimestamp && elapsed <= FETCH_COOLDOWN) {
            Timber.i("Skipping fetch because one was done recently.")
            return
        }
        lastFetchTimestamp = currentTimestamp

        val responseSummary = apolloWrapper.performQuery(
            query = UserInfoQuery(),
            userMustBeLoggedIn = false
        )
        val response = responseSummary.response
        val exception = responseSummary.exception

        // Clear UserInfo if the exception is not caused by a user's bad network connectivity
        if (
            exception != null &&
            (exception !is ApolloNetworkException && exception !is IllegalStateException)
        ) {
            clearUserInfo()
            Timber.e(
                t = exception,
                message = "Could not perform UserInfoQuery fetch"
            )
            return
        }

        response?.data?.user?.let { userQuery ->
            if (response.hasErrors()) {
                clearUserInfo()
            } else {
                setUserInfo(userQuery.toUserInfo())
            }
        }
    }

    companion object {
        private val FETCH_COOLDOWN = TimeUnit.MINUTES.toMillis(1)

        fun UserInfoQuery.User.toUserInfo(): UserInfo {
            return UserInfo(
                id = id,
                displayName = profile.displayName,
                email = profile.email,
                pictureURL = profile.pictureURL,
                ssoProviderString = (
                    SSOProvider.values()
                        .firstOrNull { it.url == authProvider }
                        ?: SSOProvider.UNKNOWN
                    ).name,
                subscriptionTypeString = (
                    SubscriptionType.values()
                        .firstOrNull { it == subscriptionType }
                        ?: SubscriptionType.Unknown
                    ).name,
                subscriptionSourceString = (
                    SubscriptionSource.values()
                        .firstOrNull { it == subscription?.source }
                        ?: SubscriptionSource.UNKNOWN__
                    ).name,
            )
        }
    }
}

class PreviewNeevaUser(
    loginToken: LoginToken
) : NeevaUser(loginToken = loginToken) {
    override fun setUserInfo(newData: UserInfo) {}
    override fun clearUserInfo() {}

    override fun isSignedOut(): Boolean = false

    override suspend fun fetch(
        apolloWrapper: ApolloWrapper,
        context: Context,
        ignoreLastFetchTimestamp: Boolean
    ) {}

    override fun queueOnSignIn(uniqueJobName: String, job: suspend () -> Unit) {}
}
