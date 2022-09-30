// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import android.content.Context
import android.util.Log
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.apollo.ApolloWrapper
import com.neeva.app.apollo.UnauthenticatedApolloWrapper
import com.neeva.app.contentfilter.IncognitoTrackersAllowList
import com.neeva.app.contentfilter.ScriptInjectionManager
import com.neeva.app.neevascope.NeevaScopeModel
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.storage.Directories
import com.neeva.app.storage.IncognitoTabScreenshotManager
import com.neeva.app.storage.favicons.IncognitoFaviconCache
import com.neeva.app.ui.PopupModel
import com.neeva.app.userdata.IncognitoSessionToken
import com.neeva.app.userdata.NeevaUser
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.withContext
import org.chromium.weblayer.Browser
import org.chromium.weblayer.Profile
import org.chromium.weblayer.Tab
import org.chromium.weblayer.WebLayer

/** Maintains the logic for an Incognito browser profile. */
class IncognitoBrowserWrapper private constructor(
    appContext: Context,
    activityCallbackProvider: ActivityCallbackProvider,
    private val authenticatedApolloWrapper: ApolloWrapper,
    private val unauthenticatedApolloWrapper: UnauthenticatedApolloWrapper,
    coroutineScope: CoroutineScope,
    dispatchers: Dispatchers,
    domainProvider: DomainProvider,
    private val incognitoFaviconCache: IncognitoFaviconCache,
    neevaConstants: NeevaConstants,
    private val onRemovedFromHierarchy: (IncognitoBrowserWrapper) -> Unit,
    settingsDataModel: SettingsDataModel,
    tabScreenshotManager: IncognitoTabScreenshotManager,
    scriptInjectionManager: ScriptInjectionManager,
    sharedPreferencesModel: SharedPreferencesModel,
    popupModel: PopupModel,
    neevaUser: NeevaUser,
    private val incognitoSessionToken: IncognitoSessionToken
) : BaseBrowserWrapper(
    isIncognito = true,
    appContext = appContext,
    coroutineScope = coroutineScope,
    dispatchers = dispatchers,
    activityCallbackProvider = activityCallbackProvider,
    suggestionsModel = null,
    neevaScopeModel = NeevaScopeModel(
        apolloWrapper = unauthenticatedApolloWrapper,
        coroutineScope = coroutineScope,
        dispatchers = dispatchers,
        appContext = appContext
    ),
    popupModel = popupModel,
    neevaUser = neevaUser,
    faviconCache = incognitoFaviconCache,
    spaceStore = null,
    historyManager = null,
    tabScreenshotManager = tabScreenshotManager,
    domainProvider = domainProvider,
    neevaConstants = neevaConstants,
    sharedPreferencesModel = sharedPreferencesModel,
    settingsDataModel = settingsDataModel,
    trackerAllowList = IncognitoTrackersAllowList(),
    scriptInjectionManager = scriptInjectionManager,
    tabList = IncognitoTabList(),
    clientLogger = null
) {
    constructor(
        appContext: Context,
        coroutineScope: CoroutineScope,
        dispatchers: Dispatchers,
        activityCallbackProvider: ActivityCallbackProvider,
        authenticatedApolloWrapper: ApolloWrapper,
        unauthenticatedApolloWrapper: UnauthenticatedApolloWrapper,
        domainProvider: DomainProvider,
        onRemovedFromHierarchy: (IncognitoBrowserWrapper) -> Unit,
        neevaConstants: NeevaConstants,
        scriptInjectionManager: ScriptInjectionManager,
        sharedPreferencesModel: SharedPreferencesModel,
        settingsDataModel: SettingsDataModel,
        directories: Directories,
        tempDirectory: Deferred<File> = directories.cacheSubdirectoryAsync(FOLDER_NAME),
        popupModel: PopupModel,
        neevaUser: NeevaUser,
        incognitoSessionToken: IncognitoSessionToken
    ) : this(
        appContext = appContext,
        activityCallbackProvider = activityCallbackProvider,
        authenticatedApolloWrapper = authenticatedApolloWrapper,
        unauthenticatedApolloWrapper = unauthenticatedApolloWrapper,
        coroutineScope = coroutineScope,
        dispatchers = dispatchers,
        domainProvider = domainProvider,
        incognitoFaviconCache = IncognitoFaviconCache(
            appContext,
            tempDirectory,
            domainProvider,
            dispatchers
        ),
        neevaConstants = neevaConstants,
        onRemovedFromHierarchy = onRemovedFromHierarchy,
        scriptInjectionManager = scriptInjectionManager,
        sharedPreferencesModel = sharedPreferencesModel,
        settingsDataModel = settingsDataModel,
        tabScreenshotManager = IncognitoTabScreenshotManager(
            appContext = appContext,
            filesDir = tempDirectory,
            coroutineScope = coroutineScope,
            dispatchers = dispatchers
        ),
        popupModel = popupModel,
        neevaUser = neevaUser,
        incognitoSessionToken = incognitoSessionToken
    )

    companion object {
        val TAG = IncognitoBrowserWrapper::class.simpleName

        internal const val INCOGNITO_PROFILE_NAME = "incognito"
        private const val INCOGNITO_PERSISTENCE_ID = "incognito_persistence_id"

        const val FOLDER_NAME = INCOGNITO_PROFILE_NAME

        /** Gets the incognito user profile from WebLayer. The Browser does not need to be alive. */
        fun getProfile(webLayer: WebLayer) = webLayer.getIncognitoProfile(INCOGNITO_PROFILE_NAME)

        suspend fun cleanUpIncognito(
            dispatchers: Dispatchers,
            incognitoProfile: Profile?,
            cacheCleaner: CacheCleaner,
            incognitoSessionToken: IncognitoSessionToken
        ) {
            // Tell WebLayer that it should destroy the incognito profile when it can.  This deletes
            // temporary files or cookies that were created while the user was in that session.
            withContext(dispatchers.main) {
                suspendCoroutine<Boolean> { continuation ->
                    incognitoProfile?.apply {
                        Log.d(TAG, "Marking incognito profile for deletion")
                        destroyAndDeleteDataFromDiskSoon {
                            Log.d(TAG, "Destroyed incognito profile")
                            continuation.resume(true)
                        }
                    } ?: run {
                        continuation.resume(true)
                    }
                }
            }

            incognitoSessionToken.purgeCachedCookie()

            // Delete temporary files we created to store Incognito favicons and tab screenshots.
            withContext(dispatchers.io) {
                cacheCleaner.run()
            }
        }
    }

    override fun createBrowserFragment() = WebLayer.createBrowserFragmentWithIncognitoProfile(
        INCOGNITO_PROFILE_NAME,
        INCOGNITO_PERSISTENCE_ID
    )

    override fun registerBrowserCallbacks(browser: Browser): Boolean {
        val wasRegistered = super.registerBrowserCallbacks(browser)
        if (!wasRegistered) return false

        incognitoSessionToken.initializeCookieManager(browser)
        return true
    }

    override fun unregisterBrowserAndTabCallbacks() {
        super.unregisterBrowserAndTabCallbacks()
        onRemovedFromHierarchy(this)
    }

    override fun onBlankTabCreated(tab: Tab) {
        // We don't want to leave the user in a state where an `about:blank` tab exists.
        tab.dispatchBeforeUnloadAndClose()
    }
}
