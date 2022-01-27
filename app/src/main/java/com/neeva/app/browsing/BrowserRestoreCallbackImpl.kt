package com.neeva.app.browsing

import android.net.Uri
import com.neeva.app.NeevaConstants
import org.chromium.weblayer.Browser
import org.chromium.weblayer.BrowserRestoreCallback

/** Handles anything that needs to be done after a Browser finishes restoration. */
class BrowserRestoreCallbackImpl(
    private val browser: Browser,
    private val cleanCache: () -> Unit,
    private val onEmptyTabList: () -> Unit
) : BrowserRestoreCallback() {
    override fun onRestoreCompleted() {
        super.onRestoreCompleted()

        if (browser.tabs.count() == 1 &&
            browser.activeTab == browser.tabs.first() &&
            browser.activeTab?.navigationController?.navigationListCurrentIndex == -1
        ) {
            browser.activeTab?.navigationController?.navigate(Uri.parse(NeevaConstants.appURL))
        } else if (browser.tabs.isEmpty()) {
            onEmptyTabList()
        }

        cleanCache()
    }
}
