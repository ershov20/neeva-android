package com.neeva.app.browsing

import kotlinx.coroutines.flow.StateFlow

data class FindInPageInfo(
    val text: String? = null,
    val activeMatchIndex: Int = 0,
    val numberOfMatches: Int = 0,
    val finalUpdate: Boolean = false
)

/** Tracks the current Find In Page status. */
interface FindInPageModel {
    val findInPageInfo: StateFlow<FindInPageInfo>
}
