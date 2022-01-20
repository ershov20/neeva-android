package com.neeva.app

import android.app.Application
import android.content.Context
import com.apollographql.apollo3.ApolloClient
import com.neeva.app.history.HistoryManager
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.publicsuffixlist.DomainProviderImpl
import com.neeva.app.settings.SettingsModel
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.storage.HistoryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
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
    fun providesApolloClient(@ApplicationContext context: Context): ApolloClient {
        return createApolloClient(context)
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
        domainProviderImpl: DomainProviderImpl
    ): HistoryManager {
        return HistoryManager(historyDatabase, domainProviderImpl)
    }

    @Provides
    @Singleton
    fun providesSpaceStore(apolloClient: ApolloClient): SpaceStore {
        return SpaceStore(apolloClient)
    }

    @Provides
    @Singleton
    fun providesSettings(application: Application): SettingsModel {
        return SettingsModel(application)
    }

    @Provides
    @Singleton
    fun providesAppNavModel(@ApplicationContext context: Context): AppNavModel {
        return AppNavModel(context)
    }
}
