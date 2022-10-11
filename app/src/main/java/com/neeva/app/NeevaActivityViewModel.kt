// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.RemoteException
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import com.apollographql.apollo3.api.Optional
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability.UPDATE_AVAILABLE
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.firstrun.FirstRunModel
import com.neeva.app.firstrun.signup.PreviewModeSignUpPrompt
import com.neeva.app.logging.ClientLogger
import com.neeva.app.logging.LogConfig
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.type.ClientLogCounterAttribute
import com.neeva.app.ui.PopupModel
import com.neeva.app.userdata.NeevaUser
import java.net.URISyntaxException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class ToolbarConfiguration(
    /** Whether the browser is showing a single toolbar or both a top and bottom toolbar. */
    val useSingleBrowserToolbar: Boolean = false,

    /** Whether or not the keyboard is open, which should remove any bottom toolbars. */
    val isKeyboardOpen: Boolean = false,

    /** Offsets the top toolbar so that it can smoothly leave and return. */
    val topControlOffset: Float = 0.0f,

    /** Offsets the bottom toolbar so that it can smoothly leave and return. */
    val bottomControlOffset: Float = 0.0f,

    /** Whether or not an app update is available to the user.  Used to badge the overflow menu. */
    val isUpdateAvailable: Boolean = false
)

class NeevaActivityViewModel(
    /** Intent that must be processed once WebLayer has finished initializing. */
    private var pendingLaunchIntent: Intent?,

    private val neevaUser: NeevaUser,
    private val spaceStore: SpaceStore,
    private val webLayerModel: WebLayerModel,
    private val popupModel: PopupModel,
    private val firstRunModel: FirstRunModel,
    private val dispatchers: Dispatchers,
    overrideCoroutineScope: CoroutineScope? = null
) : ViewModel() {
    internal val toolbarConfiguration = MutableStateFlow(ToolbarConfiguration())

    init {
        webLayerModel.currentBrowser.activeTabModel.displayedInfoFlow
            .filter { neevaUser.isSignedOut() && it.mode == ActiveTabModel.DisplayMode.QUERY }
            .filter { firstRunModel.shouldShowPreviewPromptForSignedOutQuery() }
            .flowOn(dispatchers.io)
            .onEach {
                popupModel.showBottomSheet { onDismissRequested ->
                    PreviewModeSignUpPrompt(
                        query = it.displayedText,
                        onDismiss = onDismissRequested
                    )
                }
            }
            .flowOn(dispatchers.main)
            .launchIn(overrideCoroutineScope ?: viewModelScope)
    }

    /**
     * Returns an Intent that needs to be processed when everything has been initialized.
     * Subsequent calls will return no Intent.
     *
     * This mechanism relies on the ViewModel being created and kept alive across the Activity's
     * lifecycle.  When the Activity creates the ViewModel for the first time, it is constructed
     * using the Intent used to start the Activity.  If the Activity is alive when a new Intent
     * comes in, or if the Activity is recreated due to a configuration change, the ViewModel stays
     * alive so we know not to process the Intent again.
     */
    fun getPendingLaunchIntent(): Intent? {
        val intentToReturn = pendingLaunchIntent
        pendingLaunchIntent = null
        return intentToReturn
    }

    fun determineScreenConfiguration(context: Context) {
        context.resources.apply {
            val minScreenWidth = getDimensionPixelSize(R.dimen.min_screen_width_for_one_toolbar)
            val currentScreenWidth = displayMetrics.widthPixels
            val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            toolbarConfiguration.value = toolbarConfiguration.value.copy(
                useSingleBrowserToolbar = isLandscape && currentScreenWidth >= minScreenWidth
            )
        }
    }

    fun onBottomBarOffsetChanged(offset: Int) {
        toolbarConfiguration.value = toolbarConfiguration.value.copy(
            bottomControlOffset = offset.toFloat()
        )
    }

    fun onTopBarOffsetChanged(offset: Int) {
        toolbarConfiguration.value = toolbarConfiguration.value.copy(
            topControlOffset = offset.toFloat()
        )
    }

    class Factory(
        private val pendingLaunchIntent: Intent?,
        private val neevaUser: NeevaUser,
        private val spaceStore: SpaceStore,
        private val webLayerModel: WebLayerModel,
        private val popupModel: PopupModel,
        private val firstRunModel: FirstRunModel,
        private val dispatchers: Dispatchers,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NeevaActivityViewModel::class.java)) {
                return NeevaActivityViewModel(
                    pendingLaunchIntent = pendingLaunchIntent,
                    neevaUser = neevaUser,
                    spaceStore = spaceStore,
                    webLayerModel = webLayerModel,
                    popupModel = popupModel,
                    firstRunModel = firstRunModel,
                    dispatchers = dispatchers
                ) as T
            } else {
                throw IllegalArgumentException()
            }
        }
    }

    internal fun checkForUpdates(context: Context) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "Skipping update check for debug build")
            return
        }

        val appUpdateManager = AppUpdateManagerFactory.create(context)
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { info ->
                toolbarConfiguration.value = toolbarConfiguration.value.copy(
                    isUpdateAvailable = info.updateAvailability() == UPDATE_AVAILABLE
                )
                Log.i(TAG, "Update check result: ${toolbarConfiguration.value.isUpdateAvailable}")
            }
            .addOnFailureListener {
                Log.w(TAG, "Failed to check for update", it)
            }
    }

    /** Signs the user out of the app and the website. */
    fun signOut(callback: (Boolean) -> Unit = {}) {
        viewModelScope.launch(dispatchers.io) { spaceStore.deleteAllData() }
        neevaUser.loginToken.purgeCachedCookie(callback)
    }

    fun fireExternalIntentForUri(activity: NeevaActivity, uri: Uri, closeTabIfSuccessful: Boolean) {
        try {
            val parsedIntent = when (uri.scheme) {
                "intent" -> Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                "android-app" -> Intent.parseUri(uri.toString(), Intent.URI_ANDROID_APP_SCHEME)
                else -> Intent(Intent.ACTION_VIEW, uri)
            }

            activity.startActivity(parsedIntent)
            if (closeTabIfSuccessful) activity.onBackPressed()
        } catch (e: ActivityNotFoundException) {
            // This is a no-op because we expect WebLayer to do something in the case where it fails
            // to fire out an Intent.  See onNavigationFailed inside of [TabCallbacks] for details.
        } catch (e: URISyntaxException) {
            var urlString = uri.toString()
            if (urlString.length > 100) {
                urlString = "${urlString.take(100)}..."
            }
            popupModel.showSnackbar(activity.getString(R.string.error_url_failure, urlString))
        }
    }

    fun onKeyboardStateChanged(isKeyboardOpen: Boolean) {
        toolbarConfiguration.value = toolbarConfiguration.value.copy(
            isKeyboardOpen = isKeyboardOpen
        )
    }

    private lateinit var referrerClient: InstallReferrerClient

    fun requestInstallReferrer(activity: NeevaActivity, clientLogger: ClientLogger) {
        referrerClient = InstallReferrerClient.newBuilder(activity).build()
        referrerClient.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        // Connection established.
                        try {
                            val response: ReferrerDetails = referrerClient.installReferrer
                            val referrerUrl: String = response.installReferrer

                            clientLogger.logCounter(
                                LogConfig.Interaction.REQUEST_INSTALL_REFERRER,
                                listOf(
                                    ClientLogCounterAttribute(
                                        Optional.presentIfNotNull(
                                            LogConfig.FirstRunAttributes
                                                .REFERRER_RESPONSE.attributeName
                                        ),
                                        Optional.presentIfNotNull("OK")
                                    ),
                                    ClientLogCounterAttribute(
                                        Optional.presentIfNotNull(
                                            LogConfig.FirstRunAttributes
                                                .INSTALL_REFERRER.attributeName
                                        ),
                                        Optional.presentIfNotNull(referrerUrl)
                                    )
                                )
                            )
                        } catch (e: RemoteException) {
                            Log.e(TAG, "message", e)
                        }

                        referrerClient.endConnection()
                    }
                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                        // API not available on the current Play Store app.
                        clientLogger.logCounter(
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
                        clientLogger.logCounter(
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

            override fun onInstallReferrerServiceDisconnected() {
            }
        })
    }

    companion object {
        private val TAG = NeevaActivityViewModel::class.simpleName
    }
}
