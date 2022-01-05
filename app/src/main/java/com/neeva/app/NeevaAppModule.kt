package com.neeva.app

import android.content.Context
import com.apollographql.apollo3.ApolloClient
import com.neeva.app.publicsuffixlist.SuffixListManager
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
    fun provideSuffixListManager(@ApplicationContext context: Context): SuffixListManager {
        return SuffixListManager(context)
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
}
