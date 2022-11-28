// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import kotlinx.coroutines.flow.StateFlow
import org.chromium.weblayer.Browser
import org.chromium.weblayer.Tab
import timber.log.Timber

fun Browser?.takeIfAlive(): Browser? {
    return when {
        this == null -> {
            Timber.i("Browser is null", Throwable())
            null
        }

        this.isDestroyed -> {
            Timber.w("Browser is destroyed", Throwable())
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
