package com.neeva.app

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
class CoroutineModule {
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
}
