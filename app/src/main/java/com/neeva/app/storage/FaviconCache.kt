package com.neeva.app.storage

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.math.BigInteger
import java.security.MessageDigest
import okhttp3.internal.closeQuietly

/**
 * Saves favicons into the app's cache directory.
 *
 * WebLayer doesn't provide us with URLs for the favicons they fetch -- we only get Bitmaps.  To
 * allow us to cache these and share them amongst multiple sites, we store them in the cache
 * directory with filenames determined by their MD5 hash.  Consumers may get the favicon Bitmap back
 * by loading File URIs that point at the cached file.
 *
 * TODO(dan.alcantara): Investigate using Profile.getCachedFaviconForPageUri instead.
 */
class FaviconCache(context: Context) {
    companion object {
        const val FAVICON_SUBDIRECTORY = "favicons"
        val TAG = FaviconCache::class.simpleName
    }

    private val faviconDirectory = File(context.cacheDir, FAVICON_SUBDIRECTORY)

    @WorkerThread
    suspend fun saveFavicon(bitmap: Bitmap?): Favicon? {
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
            outputStream = FileOutputStream(md5File)
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
}
