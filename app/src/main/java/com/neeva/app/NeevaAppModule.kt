package com.neeva.app

import android.content.Context
import com.neeva.app.history.HistoryManager
import com.neeva.app.logging.ClientLogger
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.publicsuffixlist.DomainProviderImpl
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.HistoryDatabase
import com.neeva.app.ui.SnackbarModel
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
    fun providesApolloWrapper(
        neevaUserToken: NeevaUserToken,
        coroutineScope: CoroutineScope,
        dispatchers: Dispatchers
    ): ApolloWrapper {
        return ApolloWrapper(neevaUserToken, null, coroutineScope, dispatchers)
    }

    @Provides
    @Singleton
    fun providesClientLogger(apolloWrapper: ApolloWrapper): ClientLogger {
        return ClientLogger(apolloWrapper)
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
        apolloWrapper: ApolloWrapper,
        neevaUser: NeevaUser,
        snackbarModel: SnackbarModel,
        dispatchers: Dispatchers
    ): SpaceStore {
        return SpaceStore(context, apolloWrapper, neevaUser, snackbarModel, dispatchers)
    }

    @Provides
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
    fun providesSettingsDataModel(
        sharedPreferencesModel: SharedPreferencesModel,
    ): SettingsDataModel {
        return SettingsDataModel(
            sharedPreferencesModel = sharedPreferencesModel
        )
    }

    @Provides
    fun providesLocalEnvironment(
        dispatchers: Dispatchers,
        domainProvider: DomainProvider,
        historyManager: HistoryManager,
        neevaUser: NeevaUser,
        settingsDataModel: SettingsDataModel,
        sharedPreferencesModel: SharedPreferencesModel,
        snackbarModel: SnackbarModel,
        spaceStore: SpaceStore,
        apolloWrapper: ApolloWrapper
    ): LocalEnvironmentState {
        return LocalEnvironmentState(
            dispatchers = dispatchers,
            domainProvider = domainProvider,
            historyManager = historyManager,
            neevaUser = neevaUser,
            settingsDataModel = settingsDataModel,
            sharedPreferencesModel = sharedPreferencesModel,
            snackbarModel = snackbarModel,
            spaceStore = spaceStore,
            apolloWrapper = apolloWrapper
        )
    }
}
