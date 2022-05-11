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
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import okhttp3.internal.closeQuietly

/** Helper for writing and reading [Bitmap]s to/from disk with different ways to define filename */
object BitmapIO {
    const val DATA_URI_PREFIX = "data:image/jpeg;base64,"
    private val TAG = BitmapIO::class.simpleName

    @WorkerThread
    fun saveBitmap(
        directory: File,
        bitmapFile: File,
        getOutputStream: (File) -> OutputStream,
        bitmap: Bitmap
    ): File? {
        return saveBitmap(
            directory = directory,
            bitmapFile = bitmapFile,
            getOutputStream = getOutputStream
        ) {
            it.write(bitmap.toByteArray())
        }
    }

    @WorkerThread
    fun saveBitmap(
        directory: File,
        bitmapFile: File,
        getOutputStream: (File) -> OutputStream,
        writeToStream: (BufferedOutputStream) -> Unit
    ): File? {
        // Write the bitmap out to storage.
        var outputStream: OutputStream? = null
        var bufferedOutputStream: BufferedOutputStream? = null
        return try {
            directory.mkdirs()
            outputStream = getOutputStream(bitmapFile)
            bufferedOutputStream = BufferedOutputStream(outputStream)
            writeToStream(bufferedOutputStream)
            bufferedOutputStream.flush()
            bitmapFile
        } catch (e: IOException) {
            Log.e(TAG, "Failed to write bitmap to storage; deleting the attempt")
            if (bitmapFile.exists()) {
                bitmapFile.delete()
            }
            null
        } finally {
            bufferedOutputStream?.closeQuietly()
            outputStream?.closeQuietly()
        }
    }

    @WorkerThread
    fun loadBitmap(
        fileUri: Uri?,
        getInputStream: (File) -> InputStream
    ): Bitmap? {
        val file = fileUri?.toFile() ?: return null
        return loadBitmap(file, getInputStream)
    }

    @WorkerThread
    fun loadBitmap(
        file: File,
        getInputStream: (File) -> InputStream
    ): Bitmap? {
        var inputStream: InputStream? = null
        var bufferedStream: BufferedInputStream? = null
        return try {
            inputStream = getInputStream(file)
            bufferedStream = BufferedInputStream(inputStream)
            BitmapFactory.decodeStream(bufferedStream)
        } catch (e: FileNotFoundException) {
            Log.d(TAG, "Could not find file", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore bitmap", e)
            null
        } finally {
            bufferedStream?.closeQuietly()
            inputStream?.closeQuietly()
        }
    }
}

fun Bitmap.toByteArray(): ByteArray {
    val stream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, 100, stream)
    return stream.toByteArray()
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

/** Returns an image of a single letter. */
fun String.toLetterBitmap(textSizeRatio: Float, backgroundColor: Int): Bitmap {
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
