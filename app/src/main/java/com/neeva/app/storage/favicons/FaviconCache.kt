// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.storage.favicons

import android.graphics.Bitmap
import android.net.Uri
import androidx.annotation.CallSuper
import androidx.annotation.WorkerThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.core.net.toUri
import com.neeva.app.Dispatchers
import com.neeva.app.previewDispatchers
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.publicsuffixlist.previewDomainProvider
import com.neeva.app.storage.BitmapIO
import com.neeva.app.storage.entities.Favicon
import com.neeva.app.storage.entities.Favicon.Companion.toBitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.withContext
import org.chromium.weblayer.Profile
import timber.log.Timber

/**
 * Saves favicons to the [faviconDirectory].
 *
 * WebLayer doesn't provide us with URLs for the favicons they fetch -- we only get Bitmaps.  To
 * allow us to cache these and share them amongst multiple sites, we store them in the cache
 * directory with filenames determined by their MD5 hash.  Consumers may get the favicon Bitmap back
 * by loading File URIs that point at the cached file.
 */
abstract class FaviconCache(
    val domainProvider: DomainProvider,
    private val dispatchers: Dispatchers
) {
    companion object {
        private const val FAVICON_SUBDIRECTORY = "favicons"
    }

    fun interface ProfileProvider {
        fun getProfile(): Profile?
    }

    protected abstract val parentDirectory: Deferred<File>

    private suspend fun getFaviconDirectory(): File {
        return parentDirectory.await().resolve(FAVICON_SUBDIRECTORY)
    }

    var profileProvider: ProfileProvider? = null

    @WorkerThread
    @CallSuper
    open suspend fun saveFavicon(siteUri: Uri?, bitmap: Bitmap?) = withContext(dispatchers.io) {
        // Because multiple URLs can be associated with the same favicon, create a filename
        // based on an MD5 hash of the favicon's bytes.  That will allow multiple sites to share
        // the same icon without having to explicitly track which site points at which file.
        val stream = ByteArrayOutputStream()
        bitmap?.compress(Bitmap.CompressFormat.PNG, 100, stream) ?: return@withContext null
        val bitmapBytes = stream.toByteArray()

        val hashBytes = MessageDigest.getInstance("MD5").digest(bitmapBytes)
        val filename = BigInteger(1, hashBytes).toString(Character.MAX_RADIX)

        // If the file exists, we can skip writing out the bitmap because the filename was based on
        // the bytes of the bitmap file that was written out.
        val file = File(getFaviconDirectory(), filename)
        try {
            if (file.exists()) {
                return@withContext Favicon(
                    faviconURL = file.toUri().toString(),
                    width = bitmap.width,
                    height = bitmap.height
                )
            }
        } catch (e: SecurityException) {
            Timber.e("Failed to check if bitmap exists: ${file.absolutePath}", e)
            return@withContext null
        }

        // Write the bitmap out to storage.
        val bitmapFile = BitmapIO.saveBitmap(getFaviconDirectory(), file, ::getOutputStream) {
            it.write(bitmapBytes)
        }

        return@withContext Favicon(
            faviconURL = bitmapFile?.toUri()?.toString(),
            width = bitmap.width,
            height = bitmap.height
        )
    }

    suspend fun loadFavicon(fileUri: Uri): Bitmap? {
        return BitmapIO.loadBitmap(fileUri, ::getInputStream)
    }

    open fun getInputStream(file: File): InputStream = FileInputStream(file)
    open fun getOutputStream(file: File): OutputStream = FileOutputStream(file)

    /**
     * Returns a [Favicon] that corresponds to the given [siteUri].
     *
     * @param siteUri Uri of the site we want a favicon for.
     * @param generate If true and no suitable favicon is found, creates a generic favicon based on
     *                 the [uri] contents.
     * @return Favicon that corresponds to the given [siteUri], or null if none could be found.
     */
    suspend fun getFavicon(siteUri: Uri?, generate: Boolean): Bitmap? {
        val cachedFavicon = getCachedFavicon(siteUri)
        if (cachedFavicon != null) return cachedFavicon

        // Create a favicon based off the URL and the first letter of the registered domain.
        return if (generate) {
            generateFavicon(siteUri)
        } else {
            null
        }
    }

    suspend fun getCachedFavicon(siteUri: Uri?): Bitmap? {
        // Check if the bitmap is stored inside of WebLayer's cache.  The callback mechanism used by
        // WebLayer is normally asynchronous, but we force it to be a suspending function so
        // that the code flows logically.
        if (siteUri != null) {
            val weblayerBitmap = withContext(dispatchers.main) {
                suspendCoroutine<Bitmap?> { continuation ->
                    profileProvider?.getProfile()?.getCachedFaviconForPageUri(siteUri) {
                        continuation.resume(it)
                    } ?: run {
                        continuation.resume(null)
                    }
                }
            }

            if (weblayerBitmap != null) return weblayerBitmap
        }

        // If WebLayer doesn't know about it, check if we stored a suitable favicon ourselves.
        return withContext(dispatchers.io) {
            getFaviconFromHistory(siteUri)
        }
    }

    suspend fun generateFavicon(siteUri: Uri?): Bitmap? = withContext(dispatchers.io) {
        return@withContext siteUri
            ?.let {
                val registeredDomain = domainProvider.getRegisteredDomain(it)
                Uri.Builder().scheme(it.scheme).authority(registeredDomain).build()
            }
            ?.toBitmap()
    }

    /** Returns a cached bitmap resulting from the user having visited the site previously. */
    protected abstract suspend fun getFaviconFromHistory(siteUri: Uri?): Bitmap?

    /** Returns a [State] that can be used in a Composable. */
    @Composable
    fun getFaviconAsync(uri: Uri?, generateFavicon: Boolean = true): State<Bitmap?> {
        // By keying this on [uri], we can avoid recompositions until [uri] changes.  This avoids
        // infinite loops of recompositions that can be triggered via [Flow.collectAsState()].
        return produceState<Bitmap?>(initialValue = null, uri) {
            value = getFavicon(uri, generateFavicon)
        }
    }

    @WorkerThread
    suspend fun pruneCacheDirectory(usedFavicons: List<String>) {
        try {
            getFaviconDirectory().let { directory ->
                if (!directory.exists()) return

                directory.listFiles()
                    ?.filterNot { usedFavicons.contains(it.toUri().toString()) }
                    ?.forEach { it.delete() }
            }
        } catch (e: Exception) {
            Timber.e("Failed to cleanup favicon directory", e)
        }
    }
}

/** Favicon cache used for Composable previews.  Doesn't actually provide favicons. */
val previewFaviconCache: FaviconCache by lazy {
    object : FaviconCache(
        domainProvider = previewDomainProvider,
        dispatchers = previewDispatchers
    ) {
        override val parentDirectory =
            CompletableDeferred(Files.createTempDirectory(null).toFile())

        override suspend fun getFaviconFromHistory(siteUri: Uri?): Bitmap? = null
    }
}
