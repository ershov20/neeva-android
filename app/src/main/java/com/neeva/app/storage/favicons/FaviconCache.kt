package com.neeva.app.storage.favicons

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.annotation.CallSuper
import androidx.annotation.WorkerThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toFile
import androidx.core.net.toUri
import coil.ImageLoader
import coil.request.ImageRequest
import com.neeva.app.Dispatchers
import com.neeva.app.previewDispatchers
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.storage.BitmapIO
import com.neeva.app.storage.entities.Favicon
import com.neeva.app.storage.entities.Favicon.Companion.toBitmap
import com.neeva.app.storage.entities.Site
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.withContext
import org.chromium.weblayer.Profile

/**
 * Saves favicons to the [faviconDirectory].
 *
 * WebLayer doesn't provide us with URLs for the favicons they fetch -- we only get Bitmaps.  To
 * allow us to cache these and share them amongst multiple sites, we store them in the cache
 * directory with filenames determined by their MD5 hash.  Consumers may get the favicon Bitmap back
 * by loading File URIs that point at the cached file.
 */
abstract class FaviconCache(
    filesDir: File,
    val domainProvider: DomainProvider,
    private val dispatchers: Dispatchers
) {
    companion object {
        private val TAG = FaviconCache::class.simpleName
        private const val FAVICON_SUBDIRECTORY = "favicons"
    }

    fun interface ProfileProvider {
        fun getProfile(): Profile?
    }

    private val faviconDirectory = File(filesDir, FAVICON_SUBDIRECTORY)

    var profileProvider: ProfileProvider? = null

    @WorkerThread
    @CallSuper
    open suspend fun saveFavicon(siteUri: Uri?, bitmap: Bitmap?) = withContext(dispatchers.io) {
        bitmap ?: return@withContext null

        val bitmapFile = BitmapIO.saveBitmap(
            directory = faviconDirectory,
            dispatchers = dispatchers,
            bitmap = bitmap
        )

        return@withContext Favicon(
            faviconURL = bitmapFile?.toUri().toString(),
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
            val historyBitmap = getFaviconFromHistory(siteUri)
            if (historyBitmap != null) return@withContext historyBitmap

            // Create a favicon based off the URL and the first letter of the registered domain.
            if (generate) {
                siteUri
                    ?.let {
                        val registeredDomain = domainProvider.getRegisteredDomain(it)
                        Uri.Builder().scheme(it.scheme).authority(registeredDomain).build()
                    }.toBitmap()
            } else {
                null
            }
        }
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

    /**
     * Returns a [State] that can be used in a Composable based on what can be read from the given
     * site. If the site contains a valid favicon url with https:// scheme, loads that, if not falls
     * back to using the FaviconCache. */
    @Composable
    fun getFaviconAsync(context: Context, site: Site): State<Bitmap?> {
        val faviconUri = Uri.parse(site.largestFavicon?.faviconURL ?: "")
        return when (faviconUri?.scheme) {
            "https" -> produceState<Bitmap?>(initialValue = null, faviconUri) {
                value = withContext(dispatchers.io) {
                    val loader = ImageLoader(context)
                    val request = ImageRequest.Builder(context).data(faviconUri.toString()).build()
                    loader.execute(request).drawable?.toBitmap()
                }
            }
            else -> produceState<Bitmap?>(initialValue = null, Uri.parse(site.siteURL)) {
                value = getFavicon(Uri.parse(site.siteURL), true)
            }
        }
    }

    @WorkerThread
    suspend fun pruneCacheDirectory(usedFavicons: List<String>) {
        try {
            if (!faviconDirectory.exists()) return

            faviconDirectory.listFiles()
                ?.filterNot { usedFavicons.contains(it.toUri().toString()) }
                ?.forEach { it.delete() }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cleanup favicon directory", e)
        }
    }
}

/** Favicon cache used for Composable previews.  Doesn't actually provide favicons. */
val mockFaviconCache: FaviconCache by lazy {
    val domainProvider = { uri: Uri? ->
        uri?.authority?.split(".")?.takeLast(2)?.joinToString(".")
    }

    object : FaviconCache(
        filesDir = Files.createTempDirectory(null).toFile(),
        domainProvider = domainProvider,
        dispatchers = previewDispatchers
    ) {
        override suspend fun getFaviconFromHistory(siteUri: Uri?): Bitmap? = null
    }
}
