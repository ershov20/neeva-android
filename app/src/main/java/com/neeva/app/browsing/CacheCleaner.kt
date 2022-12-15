// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import com.neeva.app.storage.Directories
import timber.log.Timber

/** Cleans out unnecessary files in the cache. */
class CacheCleaner(private val directories: Directories) {

    suspend fun run() {
        // Clean up the incognito files.
        try {
            directories.cacheDirectory
                .await()
                .listFiles { _, name -> name.startsWith(IncognitoBrowserWrapper.FOLDER_NAME) }
                ?.forEach { it.deleteRecursively() }
            Timber.d("Purged incognito data from cache")
        } catch (throwable: Exception) {
            Timber.e(
                t = throwable,
                message = "Failed to clean up"
            )
        }
    }
}
