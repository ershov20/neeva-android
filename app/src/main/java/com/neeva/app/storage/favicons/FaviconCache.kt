package com.neeva.app.storage.favicons

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import androidx.annotation.CallSuper
import androidx.annotation.WorkerThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.storage.entities.Favicon
import com.neeva.app.storage.entities.Favicon.Companion.toBitmap
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.math.BigInteger
import java.nio.file.Files
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.internal.closeQuietly
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
    private val profileProvider: () -> Profile?,
    val domainProvider: DomainProvider
) {
    companion object {
        private val TAG = FaviconCache::class.simpleName
        private const val FAVICON_SUBDIRECTORY = "favicons"
    }

    private val faviconDirectory = File(filesDir, FAVICON_SUBDIRECTORY)

    @WorkerThread
    @CallSuper
    open suspend fun saveFavicon(siteUri: Uri?, bitmap: Bitmap?): Favicon? {
        bitmap ?: return null

        // Create an MD5 hash of the Bitmap that we can use to find it later.
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val bitmapBytes = stream.toByteArray()
        val hashBytes = MessageDigest.getInstance("MD5").digest(bitmapBytes)
        val md5Hash = BigInteger(1, hashBytes).toString(Character.MAX_RADIX)
        val md5File = File(faviconDirectory, md5Hash)
        val favicon = Favicon(
            faviconURL = md5File.toUri().toString(),
            encodedImage = null,
            width = bitmap.width,
            height = bitmap.height
        )

        // Don't bother writing the file out again if it already exists.
        try {
            if (md5File.exists()) return favicon
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to check if favicon exists: ${md5File.absolutePath}", e)
            return null
        }

        // Write the favicon out to storage.
        var outputStream: OutputStream? = null
        var bufferedOutputStream: BufferedOutputStream? = null
        return try {
            faviconDirectory.mkdirs()
            outputStream = getOutputStream(md5File)
            bufferedOutputStream = BufferedOutputStream(outputStream)
            bufferedOutputStream.write(bitmapBytes)
            favicon
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write favicon to storage")
            null
        } finally {
            bufferedOutputStream?.closeQuietly()
            outputStream?.closeQuietly()
        }
    }

    @WorkerThread
    suspend fun loadFavicon(fileUri: Uri): Bitmap? {
        var inputStream: InputStream? = null
        var bufferedStream: BufferedInputStream? = null
        return try {
            inputStream = getInputStream(fileUri.toFile())
            bufferedStream = BufferedInputStream(inputStream)
            BitmapFactory.decodeStream(bufferedStream)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to restore favicon from storage")
            null
        } finally {
            bufferedStream?.closeQuietly()
            inputStream?.closeQuietly()
        }
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
    private suspend fun getFavicon(siteUri: Uri?, generate: Boolean): Bitmap? {
        // Check if the bitmap is stored inside of WebLayer's cache.  The callback mechanism used by
        // WebLayer is normally asynchronous, but we force it to be a suspending function so
        // that the code flows logically.
        val weblayerBitmap = suspendCoroutine<Bitmap?> { continuation ->
            if (siteUri == null) {
                continuation.resume(null)
            } else {
                profileProvider()?.getCachedFaviconForPageUri(siteUri) {
                    continuation.resume(it)
                } ?: run {
                    continuation.resume(null)
                }
            }
        }
        if (weblayerBitmap != null) return weblayerBitmap

        // If WebLayer doesn't know about it, check if we stored a suitable favicon ourselves.
        return withContext(Dispatchers.IO) {
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
}

val mockFaviconCache: FaviconCache by lazy {
    val domainProvider = { uri: Uri? ->
        uri?.authority?.split(".")?.takeLast(2)?.joinToString(".")
    }

    object : FaviconCache(
        filesDir = Files.createTempDirectory(null).toFile(),
        profileProvider = { null },
        domainProvider = domainProvider
    ) {
        override suspend fun getFaviconFromHistory(siteUri: Uri?): Bitmap? = null
    }
}
