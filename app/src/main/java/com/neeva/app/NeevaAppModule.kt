package com.neeva.app

import android.content.Context
import com.apollographql.apollo3.ApolloClient
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.history.HistoryManager
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.publicsuffixlist.DomainProviderImpl
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.HistoryDatabase
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
    fun providesApolloClient(
        @ApplicationContext context: Context,
        neevaUserToken: NeevaUserToken
    ): ApolloClient {
        return createApolloClient(context, neevaUserToken)
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
    fun providesSpaceStore(apolloClient: ApolloClient): SpaceStore {
        return SpaceStore(apolloClient)
    }

    @Provides
    fun providesSettingsDataModel(
        sharedPreferencesModel: SharedPreferencesModel,
        neevaUserToken: NeevaUserToken
    ): SettingsDataModel {
        return SettingsDataModel(sharedPreferencesModel, neevaUserToken)
    }

    @Provides
    fun providesSharedPreferences(@ApplicationContext context: Context): SharedPreferencesModel {
        return SharedPreferencesModel(context)
    }

    @Provides
    fun providesNeevaUserToken(sharedPreferencesModel: SharedPreferencesModel): NeevaUserToken {
        return NeevaUserToken(sharedPreferencesModel)
    }

    @Provides
    @Singleton
    fun providesWebLayerModel(
        @ApplicationContext context: Context,
        domainProviderImpl: DomainProviderImpl,
        historyManager: HistoryManager,
        apolloClient: ApolloClient,
        spaceStore: SpaceStore,
        coroutineScope: CoroutineScope,
        dispatchers: Dispatchers,
        neevaUserToken: NeevaUserToken
    ): WebLayerModel {
        return WebLayerModel(
            appContext = context,
            domainProviderImpl = domainProviderImpl,
            historyManager = historyManager,
            apolloClient = apolloClient,
            spaceStore = spaceStore,
            coroutineScope = coroutineScope,
            dispatchers = dispatchers,
            neevaUserToken = neevaUserToken
        )
    }
}
