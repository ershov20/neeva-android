package com.neeva.app.storage.favicons

import android.graphics.Bitmap
import android.net.Uri
import com.neeva.app.history.HistoryManager
import com.neeva.app.publicsuffixlist.DomainProvider
import java.io.File

class RegularFaviconCache(
    filesDir: File,
    domainProvider: DomainProvider,
    private val historyManager: HistoryManager
) : FaviconCache(filesDir, domainProvider) {
    override suspend fun getFaviconFromHistory(siteUri: Uri?): Bitmap? {
        val historyFavicon = siteUri?.let { historyManager.getFaviconFromHistory(it) }
        historyFavicon?.faviconURL?.let { fileUri ->
            loadFavicon(Uri.parse(fileUri))?.let { return it }
        }

        return null
    }
}
