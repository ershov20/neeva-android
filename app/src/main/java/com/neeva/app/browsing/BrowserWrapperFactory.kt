package com.neeva.app.browsing

import android.app.Application
import com.neeva.app.AuthenticatedApolloWrapper
import com.neeva.app.Dispatchers
import com.neeva.app.history.HistoryManager
import com.neeva.app.logging.ClientLogger
import com.neeva.app.publicsuffixlist.DomainProviderImpl
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.userdata.NeevaUser
import kotlinx.coroutines.CoroutineScope

class BrowserWrapperFactory(
    private val activityCallbackProvider: ActivityCallbackProvider,
    private val application: Application,
    private val cacheCleaner: CacheCleaner,
    private val domainProviderImpl: DomainProviderImpl,
    private val historyManager: HistoryManager,
    private val apolloWrapper: AuthenticatedApolloWrapper,
    private val spaceStore: SpaceStore,
    private val dispatchers: Dispatchers,
    private val neevaUser: NeevaUser,
    private val settingsDataModel: SettingsDataModel,
    private val clientLogger: ClientLogger,
    private val sharedPreferencesModel: SharedPreferencesModel
) {
    fun createRegularBrowser(coroutineScope: CoroutineScope): RegularBrowserWrapper {
        return RegularBrowserWrapper(
            appContext = application,
            coroutineScope = coroutineScope,
            dispatchers = dispatchers,
            activityCallbackProvider = activityCallbackProvider,
            domainProvider = domainProviderImpl,
            apolloWrapper = apolloWrapper,
            historyManager = historyManager,
            spaceStore = spaceStore,
            neevaUser = neevaUser,
            settingsDataModel = settingsDataModel,
            clientLogger = clientLogger,
            sharedPreferencesModel = sharedPreferencesModel
        )
    }

    fun createIncognitoBrowser(
        coroutineScope: CoroutineScope,
        onRemovedFromHierarchy: (incognitoBrowserWrapper: IncognitoBrowserWrapper) -> Unit
    ): IncognitoBrowserWrapper {
        return IncognitoBrowserWrapper(
            appContext = application,
            cacheCleaner = cacheCleaner,
            coroutineScope = coroutineScope,
            dispatchers = dispatchers,
            activityCallbackProvider = activityCallbackProvider,
            apolloWrapper = apolloWrapper,
            domainProvider = domainProviderImpl,
            onRemovedFromHierarchy = onRemovedFromHierarchy,
            sharedPreferencesModel = sharedPreferencesModel
        )
    }
}
