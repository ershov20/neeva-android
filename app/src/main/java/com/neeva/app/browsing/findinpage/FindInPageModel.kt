package com.neeva.app.browsing.findinpage

import kotlinx.coroutines.flow.StateFlow

data class FindInPageInfo(
    val text: String? = null,
    val activeMatchIndex: Int = 0,
    val numberOfMatches: Int = 0,
    val finalUpdate: Boolean = false
)

/** Tracks the current Find In Page status. */
interface FindInPageModel {
    val findInPageInfoFlow: StateFlow<FindInPageInfo>

    fun updateFindInPageQuery(text: String?)

    fun scrollToFindInPageResult(goForward: Boolean)
}
