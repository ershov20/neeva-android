package com.neeva.app.browsing

import android.util.Log
import androidx.annotation.WorkerThread
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
                ?.forEach { recursiveDelete(it) }
            Log.d(TAG, "Purged incognito data from cache")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean up", e)
        }
    }

    @WorkerThread
    suspend fun recursiveDelete(file: File) {
        try {
            if (file.isDirectory) {
                file.listFiles()?.forEach { recursiveDelete(it) }
            }
            file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean up ${file.name}", e)
        }
    }
}
