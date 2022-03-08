package com.neeva.app.browsing

import android.net.Uri
import com.neeva.app.NeevaConstants
import org.chromium.weblayer.Browser
import org.chromium.weblayer.BrowserRestoreCallback

/** Handles anything that needs to be done after a Browser finishes restoration. */
class BrowserRestoreCallbackImpl(
    private val tabList: TabList,
    private val browser: Browser,
    private val cleanCache: () -> Unit,
    private val onEmptyTabList: () -> Unit,
    private val afterRestoreCompleted: () -> Unit
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

        // Data saved to a [Tab] is only available once WebLayer has finished restoration.
        browser.tabs.forEach {
            tabList.setPersistedInfo(it, TabInfo.PersistedData(it.data), false)
        }

        cleanCache()

        afterRestoreCompleted()
    }
}
