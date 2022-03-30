package com.neeva.app.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.core.graphics.scale
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
        getOutputStream: (File) -> OutputStream = ::FileOutputStream,
        maxSize: Int? = null
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
            getOutputStream = getOutputStream,
            maxSize = maxSize
        )
    }

    @WorkerThread
    suspend fun saveBitmap(
        directory: File,
        dispatchers: Dispatchers,
        id: String? = null,
        bitmap: Bitmap?,
        getOutputStream: (File) -> OutputStream = ::FileOutputStream,
        maxSize: Int? = null
    ) = withContext(dispatchers.io) {
        bitmap ?: return@withContext null

        val scaledBitmap = if (maxSize != null) {
            bitmap.scaleDownMaintainingAspectRatio(maxSize)
        } else {
            bitmap
        }

        // Create an MD5 hash of the Bitmap that we can use to find it later.
        val stream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
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

/** Converts the bitmap into a Base64-encoded string. */
fun Bitmap.toBase64String(): String {
    val byteArrayOutputStream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)

    val byteArray = byteArrayOutputStream.toByteArray()
    return Base64.encodeToString(byteArray, Base64.DEFAULT)
}

/**
 * Scales a bitmap down while maintaining its aspect ratio.
 *
 * Returns the original bitmap if the bitmap was already smaller than the passed in size.
 */
fun Bitmap.scaleDownMaintainingAspectRatio(maxSize: Int): Bitmap {
    val newWidth: Int
    val newHeight: Int
    return if (this.height > maxSize || this.width > maxSize) {
        // Scale the image down.
        if (this.height > this.width) {
            // oldWidth / oldHeight = newWidth / MAX_SCREENSHOT_SIZE
            newHeight = maxSize
            newWidth = (this.width.toFloat() / this.height * maxSize).toInt()
        } else {
            // oldHeight / oldWidth = newHeight / MAX_SCREENSHOT_SIZE
            newWidth = maxSize
            newHeight = (this.height.toFloat() / this.width * maxSize).toInt()
        }

        scale(newWidth, newHeight)
    } else {
        // The image is already small enough.
        this
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

/** Returns an image of a single letter. */
fun String.toBitmap(textSizeRatio: Float, backgroundColor: Int): Bitmap {
    val size = 128

    val firstElement = (this.firstOrNull() ?: "").toString()
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(backgroundColor)

    val textPaint = Paint()
    textPaint.textAlign = Paint.Align.CENTER
    textPaint.color = Color.WHITE
    textPaint.textSize = size * textSizeRatio

    val xPos = canvas.width / 2.0f
    val yPos = (canvas.height - textPaint.descent() - textPaint.ascent()) / 2.0f
    canvas.drawText(firstElement.uppercase(), xPos, yPos, textPaint)
    return bitmap
}
