package com.neeva.app.browsing

import android.net.Uri
import android.os.Bundle
import com.neeva.app.NeevaConstants
import org.chromium.weblayer.Browser
import org.chromium.weblayer.BrowserRestoreCallback
import org.chromium.weblayer.Tab

/**
 * Handles persisting and restoring of Tab IDs to Bundles.
 *
 * This class will ignore any [Tab]s from the Browser that have IDs that were not persisted to the
 * Bundle when the Activity last saved it.
 *
 * TODO(dan.alcantara): That behavior seems wrong.  The logic in onRestoreCompleted also seems wonky
 *                      because it's checking the list of tabs from the Browser and not the tabs
 *                      that we re-added to our tab list.
 */
class BrowserRestoreCallbackImpl(
    private val lastSavedInstanceState: Bundle?,
    private val browser: Browser,
    private val onEmptyTabList: () -> Unit,
    private val onNewTabAdded: (tab: Tab) -> Unit
) : BrowserRestoreCallback() {
    companion object {
        private const val KEY_PREVIOUS_TAB_GUIDS = "previousTabGuids"

        /**
         * Store the stack of previous tab GUIDs that are used to set the next active tab when a tab
         * closes. Also used to setup various callbacks again on restore.
         */
        fun onSaveInstanceState(outState: Bundle, orderedTabIdList: List<String>?) {
            val previousTabGuids = orderedTabIdList?.toTypedArray()
            outState.putStringArray(KEY_PREVIOUS_TAB_GUIDS, previousTabGuids)
        }
    }

    override fun onRestoreCompleted() {
        super.onRestoreCompleted()
        restorePreviousTabList()

        if (browser.tabs.count() == 1 &&
            browser.activeTab == browser.tabs.first() &&
            browser.activeTab?.navigationController?.navigationListCurrentIndex == -1
        ) {
            browser.activeTab?.navigationController?.navigate(Uri.parse(NeevaConstants.appURL))
        } else if (browser.tabs.isEmpty()) {
            onEmptyTabList()
        }
    }

    private fun restorePreviousTabList() {
        val previousTabGuids =
            lastSavedInstanceState?.getStringArray(KEY_PREVIOUS_TAB_GUIDS) ?: return
        browser.tabs
            .filter { previousTabGuids.contains(it.guid) }
            .forEach { onNewTabAdded(it) }
    }
}
