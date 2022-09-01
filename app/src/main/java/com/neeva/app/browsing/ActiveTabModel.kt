// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import android.net.Uri
import kotlinx.coroutines.flow.StateFlow

/** Monitors changes to the [BrowserWrapper]'s active tab and emits values related to it. */
interface ActiveTabModel {
    data class NavigationInfo(
        val navigationListSize: Int = 0,
        val canGoBackward: Boolean = false,
        val canGoForward: Boolean = false,
        val desktopUserAgentEnabled: Boolean = false
    )

    /** Tracks if is being shown a query, a URL, or the placeholder text in the URL bar. */
    enum class DisplayMode {
        URL, QUERY, PLACEHOLDER
    }

    data class DisplayedInfo(
        val mode: DisplayMode = DisplayMode.URL,

        /** Text that should be displayed to the user.  Either the current URL or current query. */
        val displayedText: String = "",
    )

    /** Tracks the URL displayed for the active tab. */
    val urlFlow: StateFlow<Uri>

    /** Tracks the vertical overscroll on the current tab. */
    val verticalOverscrollFlow: StateFlow<Float>

    /** Tracks whether current URL is in any of the user's Spaces. */
    val isCurrentUrlInSpaceFlow: StateFlow<Boolean>

    /** Tracks the list of space IDs that contain the current URL */
    val spacesContainingCurrentUrlFlow: StateFlow<List<String>>

    /** Tracks the title displayed for the active tab. */
    val titleFlow: StateFlow<String>

    /** Indicates whether the active tab can navigate backwards or forwards. */
    val navigationInfoFlow: StateFlow<NavigationInfo>

    /** Indicates how much of the website for the current page has loaded. */
    val progressFlow: StateFlow<Int>

    /** Tracks what should be displayed to the user in the URL bar. */
    val displayedInfoFlow: StateFlow<DisplayedInfo>

    /** Tracks how many web tracker scripts were blocked on the current page. */
    val trackersFlow: StateFlow<Int>
}
