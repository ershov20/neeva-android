package com.neeva.app.browsing

import android.net.Uri
import kotlinx.coroutines.flow.StateFlow

/** Monitors changes to the [BrowserWrapper]'s active tab and emits values related to it. */
interface ActiveTabModel {
    data class NavigationInfo(
        val canGoBackward: Boolean = false,
        val canGoForward: Boolean = false
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

    /** Tracks whether current URL is in any of the user's Spaces. */
    val isCurrentUrlInSpaceFlow: StateFlow<Boolean>

    /** Tracks the title displayed for the active tab. */
    val titleFlow: StateFlow<String>

    /** Indicates whether the active tab can navigate backwards or forwards. */
    val navigationInfoFlow: StateFlow<NavigationInfo>

    /** Indicates how much of the website for the current page has loaded. */
    val progressFlow: StateFlow<Int>

    /** Tracks what should be displayed to the user in the URL bar. */
    val displayedInfoFlow: StateFlow<DisplayedInfo>
}
