package com.neeva.app.browsing

import android.graphics.Bitmap
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import org.chromium.weblayer.Tab

class TabScreenshotter(private val filesDir: File) {
    companion object {
        private const val DIRECTORY_TAB_SCREENSHOTS = "tab_screenshots"
    }

    private fun getTabScreenshotDirectory(): File {
        return File(filesDir, DIRECTORY_TAB_SCREENSHOTS)
    }

    fun getTabScreenshotFile(tab: Tab): File {
        return File(getTabScreenshotDirectory(), "tab_${tab.guid}.jpg")
    }

    /** Takes a screenshot of the given [tab]. */
    fun captureAndSaveScreenshot(tab: Tab?, tabList: TabList) {
        // TODO(dan.alcantara): There appears to be a race condition that results in the Tab being
        //                      destroyed (and unusable by WebLayer) before this is called.
        if (tab == null || tab.isDestroyed) return

        tab.captureScreenShot(0.5f) { thumbnail, _ ->
            val dir = File(filesDir, DIRECTORY_TAB_SCREENSHOTS)
            dir.mkdirs()

            val file = getTabScreenshotFile(tab)
            if (file.exists()) file.delete()

            try {
                val out = FileOutputStream(file)
                thumbnail?.compress(Bitmap.CompressFormat.JPEG, 100, out)
                out.flush()
                out.close()
                tabList.updateThumbnailUri(tab.guid, file.toUri())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
