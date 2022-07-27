package com.neeva.app.browsing.toolbar

import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.browsing.urlbar.URLBarModel
import com.neeva.app.browsing.urlbar.URLBarModelState
import com.neeva.app.cookiecutter.CookieCutterModel
import com.neeva.app.overflowmenu.OverflowMenuItemId
import kotlinx.coroutines.flow.StateFlow

abstract class BrowserToolbarModel {
    abstract fun goBack()
    abstract fun goForward()
    abstract fun reload()
    abstract fun reloadAfterContentFilterAllowListUpdate()
    abstract fun showNeevascope()
    abstract fun share()
    abstract fun onAddToSpace()
    abstract fun onMenuItem(id: OverflowMenuItemId)
    abstract fun onTabSwitcher()
    abstract fun onLoadUrl(urlBarModelState: URLBarModelState)

    abstract val navigationInfoFlow: StateFlow<ActiveTabModel.NavigationInfo>
    abstract val spaceStoreHasUrlFlow: StateFlow<Boolean>

    //region URL Bar Model UI
    abstract val displayedInfoFlow: StateFlow<ActiveTabModel.DisplayedInfo>
    abstract val tabProgressFlow: StateFlow<Int>
    abstract val trackersFlow: StateFlow<Int>

    abstract val useSingleBrowserToolbar: Boolean
    abstract val isIncognito: Boolean
    abstract val isUpdateAvailable: Boolean
    //endregion

    abstract val urlBarModel: URLBarModel

    abstract val cookieCutterModel: CookieCutterModel
}
