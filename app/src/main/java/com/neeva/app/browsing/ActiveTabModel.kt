package com.neeva.app.browsing

import android.net.Uri
import kotlinx.coroutines.flow.StateFlow

/** Monitors changes to the [BrowserWrapper]'s active tab and emits values related to it. */
interface ActiveTabModel {
    data class NavigationInfo(
        val canGoBackward: Boolean = false,
        val canGoForward: Boolean = false
    )

    /** Tracks the URL displayed for the active tab. */
    val urlFlow: StateFlow<Uri>

    /** Tracks whether current URL is in any of the user's Spaces. */
    val currentUrlInSpaceFlow: StateFlow<Boolean>

    /** Tracks the title displayed for the active tab. */
    val titleFlow: StateFlow<String>

    /** Indicates whether the active tab can navigate backwards or forwards. */
    val navigationInfoFlow: StateFlow<NavigationInfo>

    /** Indicates how much of the website for the current page has loaded. */
    val progressFlow: StateFlow<Int>

    /** Text that should be displayed to the user. */
    val displayedText: StateFlow<String>

    /** Tracks whether or not the user is being shown a query in the URL bar. */
    val isShowingQuery: StateFlow<Boolean>
}
