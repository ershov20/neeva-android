// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.logging

import android.net.Uri
import android.os.RemoteException
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.ReferrerDetails
import com.apollographql.apollo3.api.Optional
import com.neeva.app.BuildConfig
import com.neeva.app.Dispatchers
import com.neeva.app.LogMutation
import com.neeva.app.NeevaBrowser
import com.neeva.app.NeevaConstants
import com.neeva.app.apollo.AuthenticatedApolloWrapper
import com.neeva.app.browsing.isNeevaSearchUri
import com.neeva.app.browsing.isNeevaUri
import com.neeva.app.firstrun.FirstRunModel
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.type.ClientLog
import com.neeva.app.type.ClientLogBase
import com.neeva.app.type.ClientLogCounter
import com.neeva.app.type.ClientLogCounterAttribute
import com.neeva.app.type.ClientLogEnvironment
import com.neeva.app.type.ClientLogInput
import com.neeva.app.userdata.LoginToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

enum class ClientLoggerStatus {
    ENABLED,
    DISABLED
}

data class PendingLog(
    val path: LogConfig.Interaction,
    val attributes: List<ClientLogCounterAttribute>?
)

class ClientLogger(
    private val authenticatedApolloWrapper: AuthenticatedApolloWrapper,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers,
    private val neevaConstants: NeevaConstants,
    private val loginToken: LoginToken,
    private val sharedPreferencesModel: SharedPreferencesModel,
    private val settingsDataModel: SettingsDataModel
) {
    private val environment: ClientLogEnvironment = when {
        // We allow instrumentation tests to hit "Prod" because we inject a TestApolloClientWrapper
        // that blocks all network access while allowing us to provide predefined responses.
        NeevaBrowser.isBeingInstrumented() -> ClientLogEnvironment.Prod

        BuildConfig.DEBUG -> ClientLogEnvironment.Dev

        else -> ClientLogEnvironment.Prod
    }

    private var status: ClientLoggerStatus = ClientLoggerStatus.ENABLED

    /** Logs that weren't allowed to be sent when created.  Will be sent if/when allowed. */
    private val pendingLogs = mutableListOf<PendingLog>()
    private val pendingLogsLock = Object()

    /** Enables or disables logging, based on whether the user is in private browsing mode. */
    fun onProfileSwitch(useIncognito: Boolean) {
        status = if (useIncognito) {
            ClientLoggerStatus.DISABLED
        } else {
            ClientLoggerStatus.ENABLED
        }
    }

    fun logCounter(path: LogConfig.Interaction, attributes: List<ClientLogCounterAttribute>?) {
        coroutineScope.launch(dispatchers.io) {
            logCounterInternal(path, attributes)
        }
    }

    private suspend fun logCounterInternal(
        path: LogConfig.Interaction,
        attributes: List<ClientLogCounterAttribute>?
    ) {
        if (status != ClientLoggerStatus.ENABLED) {
            return
        }

        if (FirstRunModel.mustShowFirstRun(sharedPreferencesModel, loginToken)) {
            addPendingLog(PendingLog(path, attributes))
            return
        }

        if (!settingsDataModel.getSettingsToggleValue(SettingsToggle.LOGGING_CONSENT)) {
            Timber.i("Blocking log because logging is disabled")
            return
        }

        val mutableAttributes = attributes?.toMutableList() ?: ArrayList()
        if (LogConfig.shouldAddSessionID(path)) {
            val sessionIdAttribute = ClientLogCounterAttribute(
                Optional.presentIfNotNull(LogConfig.Attributes.SESSION_UUID_V2.attributeName),
                Optional.presentIfNotNull(LogConfig.sessionID(sharedPreferencesModel))
            )
            mutableAttributes.add(sessionIdAttribute)
        }

        // Check feature flag when we start supporting it
        val clientLogBase =
            ClientLogBase(neevaConstants.browserIdentifier, BuildConfig.VERSION_NAME, environment)
        val clientLogCounter = ClientLogCounter(
            path.interactionName,
            Optional.presentIfNotNull(mutableAttributes)
        )
        val clientLog = ClientLog(Optional.presentIfNotNull(clientLogCounter))

        if (environment == ClientLogEnvironment.Dev) {
            val attributeMap = mutableAttributes.map { "${it.key}: ${it.value}" }
            Timber.d("${path.interactionName}: ${attributeMap.joinToString(separator = ",")}")
        } else {
            val logMutation = LogMutation(
                ClientLogInput(
                    base = Optional.presentIfNotNull(clientLogBase),
                    log = listOf(clientLog)
                )
            )

            // TODO(dan.alcantara): Should we re-queue any events if the mutation fails?
            authenticatedApolloWrapper.performMutation(
                mutation = logMutation,
                userMustBeLoggedIn = false
            )
        }
    }

    private fun addPendingLog(pendingLog: PendingLog) = synchronized(pendingLogsLock) {
        pendingLogs.add(pendingLog)
    }

    /** Process any logged events that we were previously unable to send. */
    fun sendPendingLogs() {
        // Iterate on a copy of the list to prevent ConcurrentModificationExceptions.
        val copiedLogs: List<PendingLog> = synchronized(pendingLogsLock) {
            mutableListOf<PendingLog>().apply {
                addAll(pendingLogs)
                pendingLogs.clear()
            }
        }

        coroutineScope.launch(dispatchers.io) {
            copiedLogs.forEach {
                logCounterInternal(it.path, it.attributes)
            }
        }
    }

    /**
     * This logs the following navigations:
     *   - PreviewSearch: log a search event for signed out/preview user
     *   - NavigationInbound: log any navigation event within neeva.com domain except search
     *   - NavigationOutbound: log any navigation event outside of neeva.com
     */
    fun logNavigation(uri: Uri) {
        when {
            uri.isNeevaSearchUri(neevaConstants) -> {
                if (loginToken.isEmpty()) {
                    logCounter(
                        LogConfig.Interaction.PREVIEW_SEARCH,
                        null
                    )
                }
            }

            uri.isNeevaUri(neevaConstants) -> {
                logCounter(LogConfig.Interaction.NAVIGATION_INBOUND, null)
            }

            else -> {
                logCounter(LogConfig.Interaction.NAVIGATION_OUTBOUND, null)
            }
        }
    }

    /**
     * This logs the Google Play API referrer URI response
     */
    fun logReferrer(referrerClient: InstallReferrerClient, responseCode: Int) {
        when (responseCode) {
            InstallReferrerClient.InstallReferrerResponse.OK -> {
                // Connection established.
                try {
                    val response: ReferrerDetails = referrerClient.installReferrer
                    val referrerUrl: String = response.installReferrer

                    val referrerType = when {
                        referrerUrl.contains("organic") -> "organic"
                        else -> "paid"
                    }

                    logCounter(
                        LogConfig.Interaction.REQUEST_INSTALL_REFERRER,
                        listOf(
                            ClientLogCounterAttribute(
                                Optional.presentIfNotNull(
                                    LogConfig.FirstRunAttributes.REFERRER_RESPONSE.attributeName
                                ),
                                Optional.presentIfNotNull("OK")
                            ),
                            ClientLogCounterAttribute(
                                Optional.presentIfNotNull(
                                    LogConfig.FirstRunAttributes.INSTALL_REFERRER.attributeName
                                ),
                                Optional.presentIfNotNull(referrerUrl)
                            ),
                            ClientLogCounterAttribute(
                                Optional.presentIfNotNull(
                                    LogConfig.FirstRunAttributes.REFERRER_TYPE.attributeName
                                ),
                                Optional.presentIfNotNull(referrerType)
                            )
                        )
                    )
                } catch (e: RemoteException) {
                    Timber.e("Failed to get installReferrer", e)
                }

                referrerClient.endConnection()
            }
            InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                // API not available on the current Play Store app.
                logCounter(
                    LogConfig.Interaction.REQUEST_INSTALL_REFERRER,
                    listOf(
                        ClientLogCounterAttribute(
                            Optional.presentIfNotNull(
                                LogConfig.FirstRunAttributes.REFERRER_RESPONSE.attributeName
                            ),
                            Optional.presentIfNotNull("FEATURE_NOT_SUPPORTED")
                        )
                    )
                )
            }
            InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                // Connection couldn't be established.
                logCounter(
                    LogConfig.Interaction.REQUEST_INSTALL_REFERRER,
                    listOf(
                        ClientLogCounterAttribute(
                            Optional.presentIfNotNull(
                                LogConfig.FirstRunAttributes.REFERRER_RESPONSE.attributeName
                            ),
                            Optional.presentIfNotNull("SERVICE_UNAVAILABLE")
                        )
                    )
                )
            }
        }
    }
}
