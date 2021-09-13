package com.neeva.app.browsing

import android.net.Uri
import org.chromium.weblayer.Tab

val Tab.currentDisplayUrl: Uri?
    get() {
        val navigationController = navigationController
        if (navigationController.navigationListSize == 0) return null

        return navigationController.getNavigationEntryDisplayUri(
            navigationController.navigationListCurrentIndex)
    }

val Tab.currentDisplayTitle: String?
    get() {
        val navigationController = navigationController
        if (navigationController.navigationListSize == 0) return null

        return navigationController.getNavigationEntryTitle(
            navigationController.navigationListCurrentIndex)
    }

val Tab.isSelected: Boolean
    get() {
        return browser.activeTab == this
    }
