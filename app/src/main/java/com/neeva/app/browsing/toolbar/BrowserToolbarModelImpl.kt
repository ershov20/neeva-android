package com.neeva.app.browsing.toolbar

import com.neeva.app.appnav.AppNavModel
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.overflowmenu.OverflowMenuItemId

class BrowserToolbarModelImpl(
    private val appNavModel: AppNavModel,
    private val browserWrapper: BrowserWrapper
) : BrowserToolbarModel() {
    override fun goBack() = browserWrapper.goBack()
    override fun goForward() = browserWrapper.goForward()
    override fun reload() = browserWrapper.reload()

    override fun share() = appNavModel.shareCurrentPage()
    override fun onAddToSpace() = appNavModel.showAddToSpace()
    override fun onMenuItem(id: OverflowMenuItemId) = appNavModel.onMenuItem(id)

    override fun onTabSwitcher() {
        browserWrapper.takeScreenshotOfActiveTab {
            appNavModel.showCardGrid()
        }
    }

    override val navigationInfoFlow get() = browserWrapper.activeTabModel.navigationInfoFlow
    override val spaceStoreHasUrlFlow get() = browserWrapper.activeTabModel.isCurrentUrlInSpaceFlow
}
