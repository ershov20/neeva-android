package com.neeva.app.browsing

import android.net.Uri
import org.chromium.weblayer.Tab

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
