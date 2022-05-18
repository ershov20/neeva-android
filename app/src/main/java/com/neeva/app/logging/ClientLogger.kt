package com.neeva.app.logging

import com.apollographql.apollo3.api.Optional
import com.neeva.app.ApolloWrapper
import com.neeva.app.BuildConfig
import com.neeva.app.LogMutation
import com.neeva.app.NeevaConstants
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.type.ClientLog
import com.neeva.app.type.ClientLogBase
import com.neeva.app.type.ClientLogCounter
import com.neeva.app.type.ClientLogCounterAttribute
import com.neeva.app.type.ClientLogEnvironment
import com.neeva.app.type.ClientLogInput

enum class ClientLoggerStatus {
    ENABLED,
    DISABLED
}

class ClientLogger(
    private val apolloWrapper: ApolloWrapper,
    private val sharedPreferencesModel: SharedPreferencesModel,
    private val neevaConstants: NeevaConstants
) {
    private val env: ClientLogEnvironment =
        if (BuildConfig.DEBUG) ClientLogEnvironment.Dev else ClientLogEnvironment.Prod
    private var status: ClientLoggerStatus = ClientLoggerStatus.ENABLED

    /** Enables or disables logging, based on whether the user is in private browsing mode. */
    fun onProfileSwitch(useIncognito: Boolean) {
        status = if (useIncognito) {
            ClientLoggerStatus.DISABLED
        } else {
            ClientLoggerStatus.ENABLED
        }
    }

    fun logCounter(path: LogConfig.Interaction, attributes: List<ClientLogCounterAttribute>?) {
        if (status != ClientLoggerStatus.ENABLED) {
            return
        }

        val mutableAttributes = attributes?.toMutableList() ?: ArrayList()
        if (path.category == LogConfig.Category.FIRST_RUN ||
            path.category == LogConfig.Category.STABILITY
        ) {
            val sessionIdAttribute = ClientLogCounterAttribute(
                Optional.presentIfNotNull(LogConfig.Attributes.SESSION_UUID_V2.attributeName),
                Optional.presentIfNotNull(LogConfig.sessionID(sharedPreferencesModel))
            )
            mutableAttributes?.add(sessionIdAttribute)
        }

        // Check feature flag when we start supporting it
        val clientLogBase =
            ClientLogBase(neevaConstants.browserIdentifier, BuildConfig.VERSION_NAME, env)
        val clientLogCounter = ClientLogCounter(
            path.interactionName,
            Optional.presentIfNotNull(mutableAttributes)
        )
        val clientLog = ClientLog(Optional.presentIfNotNull(clientLogCounter))
        val logMutation = LogMutation(
            ClientLogInput(
                Optional.presentIfNotNull(clientLogBase),
                listOf(clientLog)
            )
        )

        apolloWrapper.performMutationAsync(mutation = logMutation, userMustBeLoggedIn = false) {}
    }
}
