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
import com.neeva.app.settings.SettingsToggle
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

@Module
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
        neevaConstants: NeevaConstants,
        coroutineScope: CoroutineScope,
        dispatchers: Dispatchers
    ): AuthenticatedApolloWrapper {
        return AuthenticatedApolloWrapper(
            neevaUserToken = neevaUserToken,
            neevaConstants = neevaConstants,
            coroutineScope = coroutineScope,
            dispatchers = dispatchers
        )
    }

    @Provides
    @Singleton
    fun providesUnauthenticatedApolloWrapper(
        neevaConstants: NeevaConstants,
        coroutineScope: CoroutineScope,
        dispatchers: Dispatchers
    ): UnauthenticatedApolloWrapper {
        return UnauthenticatedApolloWrapper(
            _apolloClient = null,
            coroutineScope = coroutineScope,
            dispatchers = dispatchers,
            neevaConstants = neevaConstants
        )
    }

    @Provides
    @Singleton
    fun providesClientLogger(
        apolloWrapper: AuthenticatedApolloWrapper,
        sharedPreferencesModel: SharedPreferencesModel,
        neevaConstants: NeevaConstants
    ): ClientLogger {
        return ClientLogger(apolloWrapper, sharedPreferencesModel, neevaConstants)
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
    fun providesDatabase(
        @ApplicationContext context: Context,
        sharedPreferencesModel: SharedPreferencesModel
    ): HistoryDatabase {
        return HistoryDatabase.create(context, sharedPreferencesModel)
    }

    @Provides
    @Singleton
    fun providesHistoryManager(
        historyDatabase: HistoryDatabase,
        domainProviderImpl: DomainProviderImpl,
        coroutineScope: CoroutineScope,
        dispatchers: Dispatchers,
        neevaConstants: NeevaConstants
    ): HistoryManager {
        return HistoryManager(
            historyDatabase = historyDatabase,
            domainProvider = domainProviderImpl,
            coroutineScope = coroutineScope,
            dispatchers = dispatchers,
            neevaConstants = neevaConstants
        )
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
        neevaConstants: NeevaConstants,
        snackbarModel: SnackbarModel,
        dispatchers: Dispatchers
    ): SpaceStore {
        return SpaceStore(
            appContext = context,
            historyDatabase = historyDatabase,
            coroutineScope = coroutineScope,
            unauthenticatedApolloWrapper = unauthenticatedApolloWrapper,
            authenticatedApolloWrapper = authenticatedApolloWrapper,
            neevaUser = neevaUser,
            neevaConstants = neevaConstants,
            snackbarModel = snackbarModel,
            dispatchers = dispatchers
        )
    }

    @Provides
    @Singleton
    fun providesSharedPreferences(@ApplicationContext context: Context): SharedPreferencesModel {
        return SharedPreferencesModel(context)
    }

    @Provides
    @Singleton
    fun providesNeevaConstants(settingsDataModel: SettingsDataModel): NeevaConstants {
        // This is done during initialization so that the app consistently hits the same server during the app's lifetime.
        // To use a different host, you will need to restart the app.
        val appHost = when {
            settingsDataModel.getSettingsToggleValue(SettingsToggle.DEBUG_M1_APP_HOST) -> {
                "m1.neeva.com"
            }

            settingsDataModel
                .getSettingsToggleValue(SettingsToggle.DEBUG_LOCAL_NEEVA_DEV_APP_HOST) -> {
                "local.neeva.dev"
            }

            else -> {
                "neeva.com"
            }
        }
        return NeevaConstants(appHost = appHost)
    }

    @Provides
    @Singleton
    fun providesNeevaUser(neevaUserToken: NeevaUserToken): NeevaUser {
        return NeevaUser(neevaUserToken = neevaUserToken)
    }

    @Provides
    @Singleton
    fun providesNeevaUserToken(
        sharedPreferencesModel: SharedPreferencesModel,
        neevaConstants: NeevaConstants
    ): NeevaUserToken {
        return NeevaUserToken(
            sharedPreferencesModel = sharedPreferencesModel,
            neevaConstants = neevaConstants
        )
    }

    @Provides
    @Singleton
    fun providesBrowserWrapperFactory(
        activityCallbackProvider: ActivityCallbackProvider,
        application: Application,
        apolloWrapper: AuthenticatedApolloWrapper,
        clientLogger: ClientLogger,
        dispatchers: Dispatchers,
        domainProviderImpl: DomainProviderImpl,
        historyDatabase: HistoryDatabase,
        historyManager: HistoryManager,
        neevaConstants: NeevaConstants,
        neevaUser: NeevaUser,
        settingsDataModel: SettingsDataModel,
        spaceStore: SpaceStore,
    ): BrowserWrapperFactory {
        return BrowserWrapperFactory(
            activityCallbackProvider = activityCallbackProvider,
            application = application,
            apolloWrapper = apolloWrapper,
            clientLogger = clientLogger,
            dispatchers = dispatchers,
            domainProviderImpl = domainProviderImpl,
            historyManager = historyManager,
            hostInfoDao = historyDatabase.hostInfoDao(),
            neevaConstants = neevaConstants,
            neevaUser = neevaUser,
            settingsDataModel = settingsDataModel,
            spaceStore = spaceStore
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
        apolloWrapper: AuthenticatedApolloWrapper,
        clientLogger: ClientLogger,
        dispatchers: Dispatchers,
        domainProvider: DomainProvider,
        historyManager: HistoryManager,
        neevaConstants: NeevaConstants,
        neevaUser: NeevaUser,
        overlaySheetModel: OverlaySheetModel,
        settingsDataModel: SettingsDataModel,
        sharedPreferencesModel: SharedPreferencesModel,
        snackbarModel: SnackbarModel,
        spaceStore: SpaceStore
    ): LocalEnvironmentState {
        return LocalEnvironmentState(
            apolloWrapper = apolloWrapper,
            clientLogger = clientLogger,
            dispatchers = dispatchers,
            domainProvider = domainProvider,
            historyManager = historyManager,
            neevaConstants = neevaConstants,
            neevaUser = neevaUser,
            overlaySheetModel = overlaySheetModel,
            settingsDataModel = settingsDataModel,
            sharedPreferencesModel = sharedPreferencesModel,
            snackbarModel = snackbarModel,
            spaceStore = spaceStore
        )
    }
}
