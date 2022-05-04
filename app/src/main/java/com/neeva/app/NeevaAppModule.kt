package com.neeva.app

import android.app.Application
import android.content.Context
import com.neeva.app.browsing.ActivityCallbackProvider
import com.neeva.app.browsing.BrowserWrapperFactory
import com.neeva.app.browsing.CacheCleaner
import com.neeva.app.browsing.WebLayerFactory
import com.neeva.app.history.HistoryManager
import com.neeva.app.logging.ClientLogger
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.publicsuffixlist.DomainProviderImpl
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.HistoryDatabase
import com.neeva.app.ui.SnackbarModel
import com.neeva.app.ui.widgets.overlay.OverlaySheetModel
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.userdata.NeevaUserToken
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

@Module(
    includes = [
        CoroutineModule::class
    ]
)
@InstallIn(SingletonComponent::class)
object NeevaAppModule {
    @Provides
    @Singleton
    fun provideActivityCallbackProvider(): ActivityCallbackProvider {
        return ActivityCallbackProvider()
    }

    @Provides
    @Singleton
    fun provideCacheCleaner(@ApplicationContext context: Context): CacheCleaner {
        return CacheCleaner(context.cacheDir)
    }

    @Provides
    @Singleton
    fun provideDomainProvider(domainProviderImpl: DomainProviderImpl): DomainProvider {
        return domainProviderImpl
    }

    @Provides
    @Singleton
    fun provideDomainProviderImpl(@ApplicationContext context: Context): DomainProviderImpl {
        return DomainProviderImpl(context)
    }

    @Provides
    @Singleton
    fun providesAuthenticatedApolloWrapper(
        neevaUserToken: NeevaUserToken,
        coroutineScope: CoroutineScope,
        dispatchers: Dispatchers
    ): AuthenticatedApolloWrapper {
        return AuthenticatedApolloWrapper(
            neevaUserToken, null, coroutineScope, dispatchers
        )
    }

    @Provides
    @Singleton
    fun providesUnauthenticatedApolloWrapper(
        coroutineScope: CoroutineScope,
        dispatchers: Dispatchers
    ): UnauthenticatedApolloWrapper {
        return UnauthenticatedApolloWrapper(null, coroutineScope, dispatchers)
    }

    @Provides
    @Singleton
    fun providesClientLogger(
        apolloWrapper: AuthenticatedApolloWrapper,
        sharedPreferencesModel: SharedPreferencesModel,
    ): ClientLogger {
        return ClientLogger(apolloWrapper, sharedPreferencesModel)
    }

    @Provides
    @Singleton
    fun providesSnackbarModel(
        coroutineScope: CoroutineScope,
        dispatchers: Dispatchers
    ): SnackbarModel {
        return SnackbarModel(coroutineScope, dispatchers)
    }

    @Provides
    @Singleton
    fun providesDatabase(@ApplicationContext context: Context): HistoryDatabase {
        return HistoryDatabase.create(context)
    }

    @Provides
    @Singleton
    fun providesHistoryManager(
        historyDatabase: HistoryDatabase,
        domainProviderImpl: DomainProviderImpl,
        coroutineScope: CoroutineScope,
        dispatchers: Dispatchers
    ): HistoryManager {
        return HistoryManager(historyDatabase, domainProviderImpl, coroutineScope, dispatchers)
    }

    @Provides
    @Singleton
    fun providesSpaceStore(
        @ApplicationContext context: Context,
        historyDatabase: HistoryDatabase,
        coroutineScope: CoroutineScope,
        unauthenticatedApolloWrapper: UnauthenticatedApolloWrapper,
        authenticatedApolloWrapper: AuthenticatedApolloWrapper,
        neevaUser: NeevaUser,
        snackbarModel: SnackbarModel,
        dispatchers: Dispatchers
    ): SpaceStore {
        return SpaceStore(
            context,
            historyDatabase,
            coroutineScope,
            unauthenticatedApolloWrapper,
            authenticatedApolloWrapper,
            neevaUser,
            snackbarModel,
            dispatchers
        )
    }

    @Provides
    @Singleton
    fun providesSharedPreferences(@ApplicationContext context: Context): SharedPreferencesModel {
        return SharedPreferencesModel(context)
    }

    @Provides
    @Singleton
    fun providesNeevaUserToken(sharedPreferencesModel: SharedPreferencesModel): NeevaUserToken {
        return NeevaUserToken(sharedPreferencesModel)
    }

    @Provides
    @Singleton
    fun providesNeevaUser(neevaUserToken: NeevaUserToken): NeevaUser {
        return NeevaUser(neevaUserToken = neevaUserToken)
    }

    @Provides
    @Singleton
    fun providesBrowserWrapperFactory(
        activityCallbackProvider: ActivityCallbackProvider,
        application: Application,
        cacheCleaner: CacheCleaner,
        domainProviderImpl: DomainProviderImpl,
        historyManager: HistoryManager,
        apolloWrapper: AuthenticatedApolloWrapper,
        spaceStore: SpaceStore,
        dispatchers: Dispatchers,
        neevaUser: NeevaUser,
        settingsDataModel: SettingsDataModel,
        clientLogger: ClientLogger
    ): BrowserWrapperFactory {
        return BrowserWrapperFactory(
            activityCallbackProvider = activityCallbackProvider,
            application = application,
            cacheCleaner = cacheCleaner,
            domainProviderImpl = domainProviderImpl,
            historyManager = historyManager,
            apolloWrapper = apolloWrapper,
            spaceStore = spaceStore,
            dispatchers = dispatchers,
            neevaUser = neevaUser,
            settingsDataModel = settingsDataModel,
            clientLogger = clientLogger
        )
    }

    @Provides
    @Singleton
    fun providesOverlaySheetModel(): OverlaySheetModel {
        return OverlaySheetModel()
    }

    @Provides
    @Singleton
    fun providesSettingsDataModel(
        sharedPreferencesModel: SharedPreferencesModel
    ): SettingsDataModel {
        return SettingsDataModel(sharedPreferencesModel = sharedPreferencesModel)
    }

    @Provides
    @Singleton
    fun providesWebLayerFactory(@ApplicationContext appContext: Context): WebLayerFactory {
        return WebLayerFactory(appContext)
    }

    @Provides
    @Singleton
    fun providesLocalEnvironment(
        dispatchers: Dispatchers,
        domainProvider: DomainProvider,
        historyManager: HistoryManager,
        neevaUser: NeevaUser,
        overlaySheetModel: OverlaySheetModel,
        settingsDataModel: SettingsDataModel,
        sharedPreferencesModel: SharedPreferencesModel,
        snackbarModel: SnackbarModel,
        spaceStore: SpaceStore,
        apolloWrapper: AuthenticatedApolloWrapper
    ): LocalEnvironmentState {
        return LocalEnvironmentState(
            dispatchers = dispatchers,
            domainProvider = domainProvider,
            historyManager = historyManager,
            neevaUser = neevaUser,
            overlaySheetModel = overlaySheetModel,
            settingsDataModel = settingsDataModel,
            sharedPreferencesModel = sharedPreferencesModel,
            snackbarModel = snackbarModel,
            spaceStore = spaceStore,
            apolloWrapper = apolloWrapper
        )
    }
}
