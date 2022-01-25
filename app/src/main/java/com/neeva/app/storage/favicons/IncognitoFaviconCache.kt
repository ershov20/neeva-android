package com.neeva.app.storage.favicons

import android.graphics.Bitmap
import android.net.Uri
import com.neeva.app.browsing.FileEncrypter
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.storage.entities.Favicon
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import org.chromium.weblayer.Profile

/**
 * Manages a set of favicons for sites visited while in incognito mode.
 *
 * Instead of using the history database, we just store an in-memory map of hashed site URIs to file
 * URIs.  This avoids contaminating the user's regular profile history with what was visited while
 * using Incognito, and means that we lose track of what site the favicon was associated with when
 * the app dies.  The favicons themselves are deleted when the Incognito profile is closed or when
 * the app restarts (in case the app died in the background before incognito cleanup could occur).
 */
class IncognitoFaviconCache(
    filesDir: File,
    domainProvider: DomainProvider,
    profileProvider: () -> Profile?,
    private val encrypter: FileEncrypter
) : FaviconCache(filesDir, profileProvider, domainProvider) {
    private val faviconMap = mutableMapOf<Int, Uri>()

    override suspend fun saveFavicon(siteUri: Uri?, bitmap: Bitmap?): Favicon? {
        val favicon = super.saveFavicon(siteUri, bitmap)

        val key = domainProvider.getRegisteredDomain(siteUri)?.hashCode()
        val fileUri = favicon?.faviconURL?.let { Uri.parse(it) }
        if (key != null && favicon != null && fileUri != null) {
            faviconMap[key] = fileUri
        }

        return favicon
    }

    override suspend fun getFaviconFromHistory(siteUri: Uri?): Bitmap? {
        val key = domainProvider.getRegisteredDomain(siteUri)?.hashCode()
        val fileUri = key?.let { faviconMap[it] }
        return fileUri?.let { loadFavicon(it) }
    }

    /** Clears out the in-memory history of favicons that we've saved. */
    fun clearMapping() = faviconMap.clear()

    override fun getInputStream(file: File): InputStream = encrypter.getInputStream(file)
    override fun getOutputStream(file: File): OutputStream = encrypter.getOutputStream(file)
}
