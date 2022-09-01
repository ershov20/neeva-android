// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

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

            // If the tab doesn't have a timestamp yet (e.g. it was created before we added tab
            // archiving), set it to the current time and persist it so the tab correctly ages from
            // this point forward.
            val now = System.currentTimeMillis()
            val persistedData = TabInfo.PersistedData(it.isSelected, it.data, now)
            val mustPersist = (now == persistedData.lastActiveMs)

            tabList.setPersistedInfo(
                tab = it,
                newData = persistedData,
                persist = mustPersist
            )
        }

        cleanCache()
        tabList.pruneQueryNavigations()
        afterRestoreCompleted()
    }
}
