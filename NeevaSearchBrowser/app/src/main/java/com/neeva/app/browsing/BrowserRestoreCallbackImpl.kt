package com.neeva.app.browsing

import android.net.Uri
import android.os.Bundle
import com.neeva.app.NeevaConstants
import org.chromium.weblayer.Browser
import org.chromium.weblayer.BrowserRestoreCallback
import org.chromium.weblayer.Tab

class BrowserRestoreCallbackImpl(
    private val lastSavedInstanceState: Bundle?,
    private val browser: Browser,
    private val onEmptyTabList: () -> Unit,
    private val onNewTabAdded: (tab: Tab) -> Unit
) : BrowserRestoreCallback() {
    companion object {
        private const val KEY_PREVIOUS_TAB_GUIDS = "previousTabGuids"
    }

    /**
     * Store the stack of previous tab GUIDs that are used to set the next active tab when a tab
     * closes. Also used to setup various callbacks again on restore.
     */
    fun onSaveInstanceState(outState: Bundle, orderedTabList: List<TabInfo>?) {
        val previousTabGuids = orderedTabList?.map { it.id }?.toTypedArray()
        outState.putStringArray(KEY_PREVIOUS_TAB_GUIDS, previousTabGuids)
    }

    override fun onRestoreCompleted() {
        super.onRestoreCompleted()
        restorePreviousTabList()

        if (browser.tabs.count() == 1
            && browser.activeTab == browser.tabs.first()
            && browser.activeTab?.navigationController?.navigationListCurrentIndex == -1
        ) {
            browser.activeTab?.navigationController?.navigate(Uri.parse(NeevaConstants.appURL))
        } else if (browser.tabs.isEmpty()) {
            onEmptyTabList()
        }
    }

    private fun restorePreviousTabList() {
        // TODO(dan.alcantara): This logic will throw away a tab the Browser knows about but we
        //                      didn't save in the saved instance state.  Re-evaluate.
        val previousTabGuids = lastSavedInstanceState?.getStringArray(KEY_PREVIOUS_TAB_GUIDS) ?: return
        browser.tabs
            .filter { previousTabGuids.contains(it.guid) }
            .forEach { onNewTabAdded(it) }
    }
}