// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import android.content.Context
import android.net.Uri
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.apollo.AuthenticatedApolloWrapper
import com.neeva.app.contentfilter.RegularTrackersAllowList
import com.neeva.app.contentfilter.ScriptInjectionManager
import com.neeva.app.history.HistoryManager
import com.neeva.app.logging.ClientLogger
import com.neeva.app.neevascope.BloomFilterManager
import com.neeva.app.neevascope.NeevaScopeModel
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.Directories
import com.neeva.app.storage.RegularTabScreenshotManager
import com.neeva.app.storage.daos.HostInfoDao
import com.neeva.app.storage.daos.SearchNavigationDao
import com.neeva.app.storage.favicons.RegularFaviconCache
import com.neeva.app.suggestions.SuggestionsModel
import com.neeva.app.ui.PopupModel
import com.neeva.app.userdata.NeevaUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.chromium.weblayer.Browser
import org.chromium.weblayer.DownloadCallback
import org.chromium.weblayer.Tab
import org.chromium.weblayer.WebLayer

/**
 * Encapsulates logic for a regular browser profile, which tracks history and automatically fetches
 * suggestions from the backend as the user types out a query.
 */
class RegularBrowserWrapper(
    appContext: Context,
    activityCallbackProvider: ActivityCallbackProvider,
    private val authenticatedApolloWrapper: AuthenticatedApolloWrapper,
    clientLogger: ClientLogger,
    coroutineScope: CoroutineScope,
    directories: Directories,
    dispatchers: Dispatchers,
    domainProvider: DomainProvider,
    downloadCallback: DownloadCallback,
    historyManager: HistoryManager,
    hostInfoDao: HostInfoDao,
    searchNavigationDao: SearchNavigationDao,
    neevaConstants: NeevaConstants,
    private val neevaUser: NeevaUser,
    regularFaviconCache: RegularFaviconCache,
    scriptInjectionManager: ScriptInjectionManager,
    sharedPreferencesModel: SharedPreferencesModel,
    settingsDataModel: SettingsDataModel,
    spaceStore: SpaceStore,
    popupModel: PopupModel,
    bloomFilterManager: BloomFilterManager
) : BaseBrowserWrapper(
    isIncognito = false,
    appContext = appContext,
    coroutineScope = coroutineScope,
    dispatchers = dispatchers,
    activityCallbackProvider = activityCallbackProvider,
    suggestionsModel = SuggestionsModel(
        coroutineScope = coroutineScope,
        historyManager = historyManager,
        settingsDataModel = settingsDataModel,
        apolloWrapper = authenticatedApolloWrapper,
        dispatchers = dispatchers,
        neevaConstants = neevaConstants,
        clientLogger = clientLogger
    ),
    neevaScopeModel = NeevaScopeModel(
        apolloWrapper = authenticatedApolloWrapper,
        coroutineScope = coroutineScope,
        dispatchers = dispatchers,
        appContext = appContext,
        bloomFilterManager = bloomFilterManager,
        neevaConstants = neevaConstants,
        sharedPreferencesModel = sharedPreferencesModel
    ),
    popupModel = popupModel,
    faviconCache = regularFaviconCache,
    spaceStore = spaceStore,
    historyManager = historyManager,
    tabScreenshotManager = RegularTabScreenshotManager(
        filesDir = directories.cacheDirectory,
        coroutineScope = coroutineScope,
        dispatchers = dispatchers
    ),
    domainProvider = domainProvider,
    downloadCallback = downloadCallback,
    neevaConstants = neevaConstants,
    scriptInjectionManager = scriptInjectionManager,
    sharedPreferencesModel = sharedPreferencesModel,
    settingsDataModel = settingsDataModel,
    trackerAllowList = RegularTrackersAllowList(
        hostInfoDao = hostInfoDao,
        coroutineScope = coroutineScope,
        dispatchers = dispatchers
    ),
    tabList = RegularTabList(
        coroutineScope = coroutineScope,
        dispatchers = dispatchers,
        searchNavigationDao = searchNavigationDao
    ),
    clientLogger = clientLogger
) {
    companion object {
        /**
         * NEVER change this value or you will cause WebLayer to use a different Profile and
         * (effectively) make all of their previous tabs inaccessible.
         */
        internal const val NON_INCOGNITO_PROFILE_NAME = "DefaultProfile"
        private const val PERSISTENCE_ID = "Neeva_Browser"

        /** Asks WebLayer to get the regular user profile. The Browser does not need to be alive. */
        fun getProfile(webLayer: WebLayer) = webLayer.getProfile(NON_INCOGNITO_PROFILE_NAME)
    }

    init {
        coroutineScope.launch(dispatchers.io) {
            urlBarModel.queryTextFlow.collectLatest { queryText ->
                // Pull new suggestions from the database.
                historyManager.updateSuggestionQuery(queryText)

                // Ask the backend for suggestions appropriate for the currently typed in text.
                suggestionsModel?.getSuggestionsFromBackend(queryText)
            }
        }

        coroutineScope.launch(dispatchers.io) {
            neevaUser.loginToken.cachedValueFlow.collectLatest {
                updateUserInfo(ignoreLastFetchTimestamp = true)
            }
        }
    }

    suspend fun updateUserInfo(ignoreLastFetchTimestamp: Boolean = false) {
        if (neevaUser.isSignedOut()) {
            neevaUser.clearUserInfo()
        } else {
            // If Neeva's login cookie changes, we need to refetch the user's data.
            neevaUser.fetch(
                authenticatedApolloWrapper,
                appContext,
                ignoreLastFetchTimestamp = ignoreLastFetchTimestamp
            )
        }
    }

    override fun createBrowserFragment() =
        WebLayer.createBrowserFragment(NON_INCOGNITO_PROFILE_NAME, PERSISTENCE_ID)

    override fun registerBrowserCallbacks(browser: Browser): Boolean {
        val wasRegistered = super.registerBrowserCallbacks(browser)
        if (!wasRegistered) return false

        // Initialize the CookieManager of the regular profile so that we can keep the session
        // cookies in sync.
        authenticatedApolloWrapper.initializeCookieManager(browser)

        return true
    }

    override fun onBlankTabCreated(tab: Tab) {
        // Direct the tab to go to the home page instead of leaving it on `about:blank`.
        tab.navigationController.navigate(Uri.parse(neevaConstants.appURL))
    }
}
