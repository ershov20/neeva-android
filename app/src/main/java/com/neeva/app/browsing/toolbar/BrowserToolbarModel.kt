// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing.toolbar

import android.net.Uri
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.browsing.urlbar.URLBarModel
import com.neeva.app.browsing.urlbar.URLBarModelState
import com.neeva.app.contentfilter.ContentFilterModel
import com.neeva.app.overflowmenu.OverflowMenuItemId
import kotlinx.coroutines.flow.StateFlow

abstract class BrowserToolbarModel {
    abstract fun goBack()
    abstract fun goForward()
    abstract fun reload()
    abstract fun reloadAfterContentFilterAllowListUpdate()
    abstract fun showNeevaScope()
    abstract fun share()
    abstract fun onAddToSpace()
    abstract fun onMenuItem(id: OverflowMenuItemId)
    abstract fun onTabSwitcher()
    abstract fun onLoadUrl(urlBarModelState: URLBarModelState)

    abstract val navigationInfoFlow: StateFlow<ActiveTabModel.NavigationInfo>
    abstract val spaceStoreHasUrlFlow: StateFlow<Boolean>

    //region URL Bar Model UI
    abstract val urlFlow: StateFlow<Uri>
    abstract val displayedInfoFlow: StateFlow<ActiveTabModel.DisplayedInfo>
    abstract val tabProgressFlow: StateFlow<Int>
    abstract val trackersFlow: StateFlow<Int>

    abstract val useSingleBrowserToolbar: Boolean
    abstract val isIncognito: Boolean
    abstract val isUpdateAvailable: Boolean
    //endregion

    abstract val urlBarModel: URLBarModel

    abstract val contentFilterModel: ContentFilterModel
}
