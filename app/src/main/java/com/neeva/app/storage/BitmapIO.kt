package com.neeva.app.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.core.net.toFile
import com.neeva.app.Dispatchers
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
import java.security.MessageDigest
import kotlinx.coroutines.withContext
import okhttp3.internal.closeQuietly

/** Helper for writing and reading [Bitmap]s to/from disk with different ways to define filename */
object BitmapIO {
    const val DATA_URI_PREFIX = "data:image/jpeg;base64,"
    private val TAG = BitmapIO::class.simpleName

    suspend fun saveBitmap(
        directory: File,
        dispatchers: Dispatchers,
        id: String?,
        bitmapString: String?,
        getOutputStream: (File) -> OutputStream = ::FileOutputStream
    ) = withContext(dispatchers.io) {
        val file = File(directory, id)
        // Don't bother converting string to bitmap if the file already exists.
        try {
            if (file.exists()) return@withContext file
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to check if bitmap exists: ${file.absolutePath}", e)
            return@withContext null
        }

        return@withContext saveBitmap(
            directory = directory,
            dispatchers = dispatchers,
            id = id,
            bitmap = bitmapString?.toBitmap(),
            getOutputStream = getOutputStream
        )
    }

    @WorkerThread
    suspend fun saveBitmap(
        directory: File,
        dispatchers: Dispatchers,
        id: String? = null,
        bitmap: Bitmap?,
        getOutputStream: (File) -> OutputStream = ::FileOutputStream
    ) = withContext(dispatchers.io) {
        bitmap ?: return@withContext null

        // Create an MD5 hash of the Bitmap that we can use to find it later.
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        val bitmapBytes = stream.toByteArray()
        val filename = id ?: run {
            val hashBytes = MessageDigest.getInstance("MD5").digest(bitmapBytes)
            BigInteger(1, hashBytes).toString(Character.MAX_RADIX)
        }

        val file = File(directory, filename)
        // Don't bother writing the file out again if it already exists.
        try {
            if (file.exists()) return@withContext file
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to check if bitmap exists: ${file.absolutePath}", e)
            return@withContext null
        }

        // Write the bitmap out to storage.
        var outputStream: OutputStream? = null
        var bufferedOutputStream: BufferedOutputStream? = null
        return@withContext try {
            directory.mkdirs()
            outputStream = getOutputStream(file)
            bufferedOutputStream = BufferedOutputStream(outputStream)
            bufferedOutputStream.write(bitmapBytes)
            file
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write bitmap to storage")
            null
        } finally {
            bufferedOutputStream?.closeQuietly()
            outputStream?.closeQuietly()
        }
    }

    @WorkerThread
    fun loadBitmap(
        fileUri: Uri?,
        getInputStream: (File) -> InputStream = ::FileInputStream
    ): Bitmap? {
        fileUri ?: return null

        var inputStream: InputStream? = null
        var bufferedStream: BufferedInputStream? = null
        return try {
            inputStream = getInputStream(fileUri.toFile())
            bufferedStream = BufferedInputStream(inputStream)
            BitmapFactory.decodeStream(bufferedStream)
        } catch (e: IOException) {
            Log.e(TAG, "Failed to restore bitmap from storage")
            null
        } finally {
            bufferedStream?.closeQuietly()
            inputStream?.closeQuietly()
        }
    }
}

fun String.toBitmap(): Bitmap? {
    val encoded = this
        .takeIf { it.startsWith(BitmapIO.DATA_URI_PREFIX) }
        ?.drop(BitmapIO.DATA_URI_PREFIX.length)
        ?: return null

    return try {
        val byteArray = Base64.decode(encoded, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    } catch (e: IllegalArgumentException) {
        null
    }
}
