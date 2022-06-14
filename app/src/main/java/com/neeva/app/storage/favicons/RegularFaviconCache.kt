package com.neeva.app.storage.favicons

import android.graphics.Bitmap
import android.net.Uri
import com.neeva.app.Dispatchers
import com.neeva.app.history.HistoryManager
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.storage.Directories
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [FaviconCache] associated with the regular browser profile.
 *
 * DO NOT USE anywhere where Incognito data can get mixed with the regular profile data.
 */
@Singleton
class RegularFaviconCache @Inject constructor(
    domainProvider: DomainProvider,
    private val historyManager: HistoryManager,
    dispatchers: Dispatchers,
    directories: Directories
) : FaviconCache(domainProvider, dispatchers) {
    override val parentDirectory = directories.cacheDirectory

    override suspend fun getFaviconFromHistory(siteUri: Uri?): Bitmap? {
        val historyFavicon = siteUri?.let { historyManager.getFaviconFromHistory(it) }
        historyFavicon?.faviconURL?.let { fileUri ->
            loadFavicon(Uri.parse(fileUri))?.let { return it }
        }

        return null
    }
}
