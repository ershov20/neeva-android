// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import android.util.Log
import kotlinx.coroutines.flow.StateFlow
import org.chromium.weblayer.Browser
import org.chromium.weblayer.Tab

private const val TAG = "BrowserExtensions"

fun Browser?.takeIfAlive(): Browser? {
    return when {
        this == null -> {
            Log.i(TAG, "Browser is null", Throwable())
            null
        }

        this.isDestroyed -> {
            Log.w(TAG, "Browser is destroyed", Throwable())
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
    return takeIfAlive()?.activeTab?.takeUnless { it.isDestroyed }
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
