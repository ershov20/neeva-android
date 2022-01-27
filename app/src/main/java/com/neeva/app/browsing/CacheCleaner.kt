package com.neeva.app.browsing

import android.util.Log
import java.io.File

/** Cleans out unnecessary files in the cache. */
class CacheCleaner(private val cacheDir: File) {
    companion object {
        val TAG = CacheCleaner::class.simpleName
    }

    suspend fun run() {
        cleanupIncognito()
    }

    suspend fun cleanupIncognito() {
        try {
            cacheDir
                .listFiles { _, name -> name.startsWith(IncognitoBrowserWrapper.FOLDER_PREFIX) }
                ?.forEach { it.deleteRecursively() }
            Log.d(TAG, "Purged incognito data from cache")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean up", e)
        }
    }
}
