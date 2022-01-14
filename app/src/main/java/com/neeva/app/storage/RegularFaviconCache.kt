package com.neeva.app.storage

import android.graphics.Bitmap
import android.net.Uri
import com.neeva.app.history.HistoryManager
import com.neeva.app.publicsuffixlist.DomainProvider
import java.io.File
import org.chromium.weblayer.Profile

class RegularFaviconCache(
    filesDir: File,
    domainProvider: DomainProvider,
    profileProvider: () -> Profile?,
    private val historyManager: HistoryManager
) : FaviconCache(filesDir, profileProvider, domainProvider) {
    override suspend fun getFaviconFromHistory(siteUri: Uri?): Bitmap? {
        val historyFavicon = siteUri?.let { historyManager.getFaviconFromHistory(it) }
        historyFavicon?.faviconURL?.let { fileUri ->
            loadFavicon(Uri.parse(fileUri))?.let { return it }
        }

        return null
    }
}
