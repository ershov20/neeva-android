package com.neeva.app.browsing

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
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
open class TabScreenshotManager(private val filesDir: File) {
    companion object {
        private val TAG = TabScreenshotManager::class.simpleName
        private const val DIRECTORY_TAB_SCREENSHOTS = "tab_screenshots"
    }

    private fun getTabScreenshotDirectory(): File {
        return File(filesDir, DIRECTORY_TAB_SCREENSHOTS)
    }

    fun getTabScreenshotFile(guid: String) = File(getTabScreenshotDirectory(), "tab_$guid.jpg")
    fun getTabScreenshotFile(tab: Tab) = getTabScreenshotFile(tab.guid)

    /** Takes a screenshot of the given [tab]. */
    fun captureAndSaveScreenshot(tab: Tab?, onCompleted: () -> Unit = {}) {
        if (tab == null || tab.isDestroyed) {
            onCompleted()
            return
        }

        val tabGuid = tab.guid

        tab.captureScreenShot(0.5f) { thumbnail, _ ->
            val dir = File(filesDir, DIRECTORY_TAB_SCREENSHOTS)
            dir.mkdirs()

            val file = getTabScreenshotFile(tabGuid)
            if (file.exists()) file.delete()

            var fileStream: OutputStream? = null

            try {
                fileStream = getOutputStream(file)
                thumbnail?.compress(Bitmap.CompressFormat.JPEG, 100, fileStream)
                fileStream.flush()
                fileStream.close()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to store bitmap", e)
            } finally {
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

        return try {
            fileStream = getInputStream(file)
            BitmapFactory.decodeStream(fileStream)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore bitmap", e)
            null
        } finally {
            fileStream?.closeQuietly()
        }
    }

    open fun getInputStream(file: File): InputStream {
        return FileInputStream(file)
    }

    open fun getOutputStream(file: File): OutputStream {
        return FileOutputStream(file)
    }
}

/**
 * Manages a set of thumbnails for tabs created while in incognito mode.  These files are encrypted
 * and stored in the cache directory, then deleted when either the Incognito profile is deleted or
 * when the app is next restarted.
 */
class IncognitoTabScreenshotManager(
    private val appContext: Application,
    filesDir: File
) : TabScreenshotManager(filesDir) {
    private val masterKey: MasterKey

    init {
        val spec = KeyGenParameterSpec
            .Builder(
                MasterKey.DEFAULT_MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(MasterKey.DEFAULT_AES_GCM_MASTER_KEY_SIZE)
            .build()

        masterKey = MasterKey.Builder(appContext).setKeyGenParameterSpec(spec).build()
    }

    override fun getInputStream(file: File): InputStream {
        return EncryptedFile(appContext, file, masterKey).openFileInput()
    }

    override fun getOutputStream(file: File): OutputStream {
        return EncryptedFile(appContext, file, masterKey).openFileOutput()
    }
}
