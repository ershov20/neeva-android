package com.neeva.app.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.WorkerThread
import com.neeva.app.browsing.FileEncrypter
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import okhttp3.internal.closeQuietly
import org.chromium.weblayer.Tab

/**
 * Manages thumbnails for each tab to display in the tab switcher.  These thumbnails are created by
 * WebLayer and persisted into our cache directory.
 */
abstract class TabScreenshotManager(filesDir: File) {
    companion object {
        private val TAG = TabScreenshotManager::class.simpleName
        private const val DIRECTORY_TAB_SCREENSHOTS = "tab_screenshots"
    }

    private val tabScreenshotDirectory = File(filesDir, DIRECTORY_TAB_SCREENSHOTS)

    fun getTabScreenshotFile(guid: String) = File(tabScreenshotDirectory, "tab_$guid.jpg")
    fun getTabScreenshotFile(tab: Tab) = getTabScreenshotFile(tab.guid)

    /** Takes a screenshot of the given [tab]. */
    fun captureAndSaveScreenshot(tab: Tab?, onCompleted: () -> Unit = {}) {
        if (tab == null || tab.isDestroyed) {
            onCompleted()
            return
        }

        val tabGuid = tab.guid

        tab.captureScreenShot(0.5f) { thumbnail, _ ->
            val dir = tabScreenshotDirectory
            dir.mkdirs()

            val file = getTabScreenshotFile(tabGuid)
            if (file.exists()) file.delete()

            var fileStream: OutputStream? = null
            var bufferedStream: BufferedOutputStream? = null
            try {
                fileStream = getOutputStream(file)
                bufferedStream = BufferedOutputStream(fileStream)
                thumbnail?.compress(Bitmap.CompressFormat.JPEG, 100, bufferedStream)
                bufferedStream.flush()
                bufferedStream.close()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to store bitmap", e)
            } finally {
                bufferedStream?.closeQuietly()
                fileStream?.closeQuietly()
                onCompleted()
            }
        }
    }

    @WorkerThread
    fun deleteScreenshot(tabId: String) {
        try {
            val file = getTabScreenshotFile(tabId)
            if (file.exists()) file.delete()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete thumbnail for $tabId", e)
        }
    }

    @WorkerThread
    fun restoreScreenshot(tabId: String): Bitmap? {
        val file = getTabScreenshotFile(tabId)
        var fileStream: InputStream? = null
        var bufferedStream: BufferedInputStream? = null

        return try {
            fileStream = getInputStream(file)
            bufferedStream = BufferedInputStream(fileStream)
            BitmapFactory.decodeStream(bufferedStream)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore bitmap", e)
            null
        } finally {
            bufferedStream?.closeQuietly()
            fileStream?.closeQuietly()
        }
    }

    abstract fun getInputStream(file: File): InputStream
    abstract fun getOutputStream(file: File): OutputStream
}

/** Caches unencrypted screenshots of tabs. */
class RegularTabScreenshotManager(filesDir: File) : TabScreenshotManager(filesDir) {
    override fun getInputStream(file: File) = FileInputStream(file)
    override fun getOutputStream(file: File) = FileOutputStream(file)
}

/** Caches screenshots of tabs and encrypts them so that they can't be accessed by outside apps. */
class IncognitoTabScreenshotManager(
    filesDir: File,
    private val encrypter: FileEncrypter
) : TabScreenshotManager(filesDir) {
    override fun getInputStream(file: File): InputStream = encrypter.getInputStream(file)
    override fun getOutputStream(file: File): OutputStream = encrypter.getOutputStream(file)
}
