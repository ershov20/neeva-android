// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

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
