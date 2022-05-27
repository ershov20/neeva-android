package com.neeva.app.browsing

import android.util.Log
import kotlinx.coroutines.flow.StateFlow
import org.chromium.weblayer.Browser
import org.chromium.weblayer.Tab

private const val TAG = "BrowserExtensions"

fun Browser?.takeIfAlive(): Browser? {
    return when {
        this == null -> {
            Log.e(TAG, "Browser is null", Throwable())
            null
        }

        this.isDestroyed -> {
            Log.e(TAG, "Browser is destroyed", Throwable())
            null
        }

        else -> {
            this
        }
    }
}

fun StateFlow<Browser?>.takeIfAlive(): Browser? {
    return value.takeIfAlive()
}

fun StateFlow<Browser?>.getActiveTab(): Tab? {
    return takeIfAlive()?.activeTab
}

fun StateFlow<Browser?>.getActiveTabId(): String? {
    return getActiveTab()?.guid
}

fun StateFlow<Browser?>.getTab(id: String?): Tab? {
    if (id == null) return null
    return takeIfAlive()?.tabs?.firstOrNull { tab -> tab.guid == id }
}

fun StateFlow<Browser?>.setActiveTab(id: String?): Boolean {
    val tab: Tab? = getTab(id)
    return if (tab != null) {
        takeIfAlive()?.setActiveTab(tab)
        true
    } else {
        false
    }
}
