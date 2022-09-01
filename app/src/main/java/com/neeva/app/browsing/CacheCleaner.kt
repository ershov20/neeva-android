// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import android.util.Log
import com.neeva.app.storage.Directories

/** Cleans out unnecessary files in the cache. */
class CacheCleaner(private val directories: Directories) {
    companion object {
        val TAG = CacheCleaner::class.simpleName
    }

    suspend fun run() {
        // Clean up the incognito files.
        try {
            directories.cacheDirectory
                .await()
                .listFiles { _, name -> name.startsWith(IncognitoBrowserWrapper.FOLDER_NAME) }
                ?.forEach { it.deleteRecursively() }
            Log.d(TAG, "Purged incognito data from cache")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clean up", e)
        }
    }
}
