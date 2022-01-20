package com.neeva.app.widgets

import com.neeva.app.history.HistoryManager
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.spaces.SpaceStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Provides access to Singleton-level injectable classes for various Composables. */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ComposableSingletonEntryPoint {
    fun historyManager(): HistoryManager
    fun spaceStore(): SpaceStore
    fun domainProvider(): DomainProvider
}
