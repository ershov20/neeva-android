package com.neeva.app.browsing

import android.app.Application
import com.neeva.app.AuthenticatedApolloWrapper
import com.neeva.app.Dispatchers
import com.neeva.app.NeevaConstants
import com.neeva.app.history.HistoryManager
import com.neeva.app.logging.ClientLogger
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.daos.HostInfoDao
import com.neeva.app.storage.favicons.RegularFaviconCache
import com.neeva.app.userdata.NeevaUser
import kotlinx.coroutines.CoroutineScope

class BrowserWrapperFactory(
    private val activityCallbackProvider: ActivityCallbackProvider,
    private val application: Application,
    private val apolloWrapper: AuthenticatedApolloWrapper,
    private val clientLogger: ClientLogger,
    private val dispatchers: Dispatchers,
    private val domainProvider: DomainProvider,
    private val historyManager: HistoryManager,
    private val hostInfoDao: HostInfoDao,
    private val neevaConstants: NeevaConstants,
    private val neevaUser: NeevaUser,
    private val regularFaviconCache: RegularFaviconCache,
    private val settingsDataModel: SettingsDataModel,
    private val spaceStore: SpaceStore
) {
    fun createRegularBrowser(coroutineScope: CoroutineScope): RegularBrowserWrapper {
        return RegularBrowserWrapper(
            appContext = application,
            activityCallbackProvider = activityCallbackProvider,
            apolloWrapper = apolloWrapper,
            clientLogger = clientLogger,
            coroutineScope = coroutineScope,
            dispatchers = dispatchers,
            domainProvider = domainProvider,
            historyManager = historyManager,
            hostInfoDao = hostInfoDao,
            neevaConstants = neevaConstants,
            neevaUser = neevaUser,
            regularFaviconCache = regularFaviconCache,
            settingsDataModel = settingsDataModel,
            spaceStore = spaceStore
        )
    }

    fun createIncognitoBrowser(
        coroutineScope: CoroutineScope,
        onRemovedFromHierarchy: (incognitoBrowserWrapper: IncognitoBrowserWrapper) -> Unit
    ): IncognitoBrowserWrapper {
        return IncognitoBrowserWrapper(
            appContext = application,
            coroutineScope = coroutineScope,
            dispatchers = dispatchers,
            activityCallbackProvider = activityCallbackProvider,
            apolloWrapper = apolloWrapper,
            domainProvider = domainProvider,
            onRemovedFromHierarchy = onRemovedFromHierarchy,
            neevaConstants = neevaConstants,
            settingsDataModel = settingsDataModel
        )
    }
}
