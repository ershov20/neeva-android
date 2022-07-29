package com.neeva.app

import android.app.Application
import android.content.Context
import com.neeva.app.apollo.AuthenticatedApolloWrapper
import com.neeva.app.apollo.UnauthenticatedApolloWrapper
import com.neeva.app.browsing.ActivityCallbackProvider
import com.neeva.app.browsing.BrowserWrapperFactory
import com.neeva.app.browsing.CacheCleaner
import com.neeva.app.browsing.WebLayerFactory
import com.neeva.app.cookiecutter.ScriptInjectionManager
import com.neeva.app.history.HistoryManager
import com.neeva.app.logging.ClientLogger
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.publicsuffixlist.DomainProviderImpl
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.Directories
import com.neeva.app.storage.HistoryDatabase
import com.neeva.app.storage.favicons.RegularFaviconCache
import com.neeva.app.ui.PopupModel
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.userdata.NeevaUserToken
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

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
    fun provideCacheCleaner(directories: Directories): CacheCleaner {
        return CacheCleaner(directories)
    }

    @Provides
    @Singleton
    fun provideCoroutineScope(dispatchers: Dispatchers): CoroutineScope {
        return CoroutineScope(SupervisorJob() + dispatchers.main)
    }

    @Provides
    @Singleton
    fun provideDispatchers(): Dispatchers {
        return Dispatchers(
            main = kotlinx.coroutines.Dispatchers.Main.immediate,
            io = kotlinx.coroutines.Dispatchers.IO,
        )
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
    fun providesScriptInjectionManager(
        @ApplicationContext context: Context,
        coroutineScope: CoroutineScope,
        dispatchers: Dispatchers
    ): ScriptInjectionManager {
        return ScriptInjectionManager(context, coroutineScope, dispatchers)
    }

    @Provides
    @Singleton
    fun providesClientLogger(
        authenticatedApolloWrapper: AuthenticatedApolloWrapper,
        coroutineScope: CoroutineScope,
        dispatchers: Dispatchers,
        neevaConstants: NeevaConstants,
        neevaUserToken: NeevaUserToken,
        sharedPreferencesModel: SharedPreferencesModel,
        settingsDataModel: SettingsDataModel
    ): ClientLogger {
        return ClientLogger(
            authenticatedApolloWrapper = authenticatedApolloWrapper,
            coroutineScope = coroutineScope,
            dispatchers = dispatchers,
            neevaConstants = neevaConstants,
            neevaUserToken = neevaUserToken,
            sharedPreferencesModel = sharedPreferencesModel,
            settingsDataModel = settingsDataModel
        )
    }

    @Provides
    @Singleton
    fun providesPopupModel(
        coroutineScope: CoroutineScope,
        dispatchers: Dispatchers
    ): PopupModel {
        return PopupModel(coroutineScope, dispatchers)
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
        popupModel: PopupModel,
        dispatchers: Dispatchers,
        directories: Directories
    ): SpaceStore {
        return SpaceStore(
            appContext = context,
            historyDatabase = historyDatabase,
            coroutineScope = coroutineScope,
            unauthenticatedApolloWrapper = unauthenticatedApolloWrapper,
            authenticatedApolloWrapper = authenticatedApolloWrapper,
            neevaUser = neevaUser,
            neevaConstants = neevaConstants,
            popupModel = popupModel,
            dispatchers = dispatchers,
            directories = directories
        )
    }

    @Provides
    @Singleton
    fun providesSharedPreferences(@ApplicationContext context: Context): SharedPreferencesModel {
        return SharedPreferencesModel(context)
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
        authenticatedApolloWrapper: AuthenticatedApolloWrapper,
        unauthenticatedApolloWrapper: UnauthenticatedApolloWrapper,
        clientLogger: ClientLogger,
        directories: Directories,
        dispatchers: Dispatchers,
        domainProvider: DomainProvider,
        historyDatabase: HistoryDatabase,
        historyManager: HistoryManager,
        neevaConstants: NeevaConstants,
        neevaUser: NeevaUser,
        regularFaviconCache: RegularFaviconCache,
        scriptInjectionManager: ScriptInjectionManager,
        settingsDataModel: SettingsDataModel,
        spaceStore: SpaceStore,
        popupModel: PopupModel
    ): BrowserWrapperFactory {
        return BrowserWrapperFactory(
            activityCallbackProvider = activityCallbackProvider,
            application = application,
            apolloWrapper = apolloWrapper,
            authenticatedApolloWrapper = authenticatedApolloWrapper,
            unauthenticatedApolloWrapper = unauthenticatedApolloWrapper,
            clientLogger = clientLogger,
            directories = directories,
            dispatchers = dispatchers,
            domainProvider = domainProvider,
            historyManager = historyManager,
            hostInfoDao = historyDatabase.hostInfoDao(),
            neevaConstants = neevaConstants,
            neevaUser = neevaUser,
            regularFaviconCache = regularFaviconCache,
            scriptInjectionManager = scriptInjectionManager,
            settingsDataModel = settingsDataModel,
            spaceStore = spaceStore,
            popupModel = popupModel
        )
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
}

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
    @Provides
    @Singleton
    fun providesDatabase(
        @ApplicationContext context: Context,
        sharedPreferencesModel: SharedPreferencesModel
    ): HistoryDatabase {
        return HistoryDatabase.create(context, sharedPreferencesModel)
    }
}

@Module
@InstallIn(SingletonComponent::class)
object NeevaConstantsModule {
    @Provides
    @Singleton
    fun providesNeevaConstants(settingsDataModel: SettingsDataModel): NeevaConstants {
        // This is done during initialization so that the app consistently hits the same server
        // during the app's lifetime.  To use a different host, you will need to restart the app.
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
}

@Module
@InstallIn(SingletonComponent::class)
class ApolloModule {
    @Provides
    @Singleton
    fun providesAuthenticatedApolloWrapper(
        neevaUserToken: NeevaUserToken,
        neevaConstants: NeevaConstants
    ): AuthenticatedApolloWrapper {
        return AuthenticatedApolloWrapper(
            neevaUserToken = neevaUserToken,
            neevaConstants = neevaConstants
        )
    }

    @Provides
    @Singleton
    fun providesUnauthenticatedApolloWrapper(
        neevaConstants: NeevaConstants
    ): UnauthenticatedApolloWrapper {
        return UnauthenticatedApolloWrapper(
            neevaConstants = neevaConstants
        )
    }
}
