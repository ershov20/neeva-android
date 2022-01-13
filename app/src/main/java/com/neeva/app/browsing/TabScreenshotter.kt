package com.neeva.app.browsing

import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import org.chromium.weblayer.Tab

class TabScreenshotter(
    private val filesDir: File,
    private val onScreenshotCaptured: (guid: String, fileUri: Uri) -> Unit
) {
    companion object {
        private const val DIRECTORY_TAB_SCREENSHOTS = "tab_screenshots"
    }

    private fun getTabScreenshotDirectory(): File {
        return File(filesDir, DIRECTORY_TAB_SCREENSHOTS)
    }

    fun getTabScreenshotFile(guid: String) = File(getTabScreenshotDirectory(), "tab_$guid.jpg")
    fun getTabScreenshotFile(tab: Tab) = getTabScreenshotFile(tab.guid)

    /** Takes a screenshot of the given [tab]. */
    fun captureAndSaveScreenshot(tab: Tab?) {
        if (tab == null || tab.isDestroyed) return

        val tabGuid = tab.guid

        tab.captureScreenShot(0.5f) { thumbnail, _ ->
            val dir = File(filesDir, DIRECTORY_TAB_SCREENSHOTS)
            dir.mkdirs()

            val file = getTabScreenshotFile(tabGuid)
            if (file.exists()) file.delete()

            try {
                val out = FileOutputStream(file)
                thumbnail?.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.flush()
                out.close()
                onScreenshotCaptured(tabGuid, file.toUri())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
