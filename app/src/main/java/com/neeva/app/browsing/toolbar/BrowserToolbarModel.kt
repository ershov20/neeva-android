package com.neeva.app.browsing.toolbar

import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.overflowmenu.OverflowMenuItemId
import kotlinx.coroutines.flow.StateFlow

abstract class BrowserToolbarModel {
    abstract fun goBack()
    abstract fun goForward()
    abstract fun reload()
    abstract fun share()
    abstract fun onAddToSpace()
    abstract fun onMenuItem(id: OverflowMenuItemId)
    abstract fun onTabSwitcher()

    abstract val navigationInfoFlow: StateFlow<ActiveTabModel.NavigationInfo>
    abstract val spaceStoreHasUrlFlow: StateFlow<Boolean>
}
