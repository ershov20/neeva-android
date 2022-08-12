package com.neeva.app.browsing

import android.util.Log
import org.chromium.weblayer.Browser
import org.chromium.weblayer.BrowserRestoreCallback
import org.chromium.weblayer.Tab

/** Handles anything that needs to be done after a Browser finishes restoration. */
class BrowserRestoreCallbackImpl(
    private val tabList: TabList,
    private val browser: Browser,
    private val cleanCache: () -> Unit,
    private val onBlankTabCreated: (tab: Tab) -> Unit,
    private val onEmptyTabList: () -> Unit,
    private val afterRestoreCompleted: () -> Unit
) : BrowserRestoreCallback() {
    companion object {
        private val TAG = BrowserRestoreCallbackImpl::class.simpleName
    }

    override fun onRestoreCompleted() {
        super.onRestoreCompleted()

        val activeTab = browser.activeTab
        if (browser.tabs.count() == 1 &&
            activeTab == browser.tabs.first() &&
            activeTab?.navigationController?.navigationListCurrentIndex == -1
        ) {
            onBlankTabCreated(activeTab)
        } else if (browser.tabs.isEmpty()) {
            onEmptyTabList()
        }

        // Data saved to a [Tab] is only available once WebLayer has finished restoration.
        browser.tabs.forEach {
            if (tabList.getTabInfo(it.guid) == null) {
                Log.w(TAG, "Adding missing tab with ID ${it.guid}")
                tabList.add(it)
            }

            tabList.setPersistedInfo(
                tab = it,
                newData = TabInfo.PersistedData(it.isSelected, it.data),
                persist = false
            )
        }

        cleanCache()
        tabList.pruneQueryNavigations()
        afterRestoreCompleted()
    }
}
