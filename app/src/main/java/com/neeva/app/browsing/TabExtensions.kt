package com.neeva.app.browsing

import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import org.chromium.weblayer.Tab
import java.io.File
import java.io.FileOutputStream

val Tab.currentDisplayUrl: Uri?
    get() {
        navigationController.apply {
            return if (navigationListSize == 0) {
                null
            } else {
                getNavigationEntryDisplayUri(navigationListCurrentIndex)
            }
        }
    }

val Tab.currentDisplayTitle: String?
    get() {
        navigationController.apply {
            return if (navigationListSize == 0) {
                null
            } else {
                getNavigationEntryTitle(navigationListCurrentIndex)
            }
        }
    }

val Tab.isSelected: Boolean
    get() {
        return browser.activeTab == this
    }

/** Takes a screenshot of the given tab. */
fun Tab.captureAndSaveScreenshot(tabList: TabList) {
    // TODO(dan.alcantara): There appears to be a race condition that results in the Tab being
    //                      destroyed (and unusable by WebLayer) before this is called.
    if (isDestroyed) return

    captureScreenShot(0.5f) { thumbnail, _ ->
        val dir = WebLayerModel.getTabScreenshotDirectory()
        dir.mkdirs()

        val file = File(dir, "tab_$guid.jpg")
        if (file.exists()) file.delete()

        try {
            val out = FileOutputStream(file)
            thumbnail?.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
            tabList.updateThumbnailUri(guid, file.toUri())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}