package com.neeva.app.storage

import android.content.Context
import com.neeva.app.Dispatchers
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

@Singleton
class Directories @Inject constructor(
    @ApplicationContext context: Context,
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers
) {
    val cacheDirectory: Deferred<File> = coroutineScope.async(dispatchers.io) {
        context.cacheDir
    }

    val filesDirectory: Deferred<File> = coroutineScope.async(dispatchers.io) {
        context.filesDir
    }

    fun cacheSubdirectoryAsync(subdirectoryName: String): Deferred<File> {
        return coroutineScope.async(dispatchers.io) {
            cacheDirectory.await().resolve(subdirectoryName)
        }
    }
}
