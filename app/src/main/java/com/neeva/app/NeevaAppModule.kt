package com.neeva.app

import android.content.Context
import com.apollographql.apollo3.ApolloClient
import com.neeva.app.history.HistoryManager
import com.neeva.app.publicsuffixlist.DomainProviderImpl
import com.neeva.app.storage.HistoryDatabase
import com.neeva.app.storage.SpaceStore
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
    fun provideSuffixListManager(@ApplicationContext context: Context): DomainProviderImpl {
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
}
