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
