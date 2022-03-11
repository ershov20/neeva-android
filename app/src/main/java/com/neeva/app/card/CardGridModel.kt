package com.neeva.app.card

import com.neeva.app.appnav.AppNavModel
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.TabInfo
import com.neeva.app.browsing.WebLayerModel

interface CardGridModel {
    fun switchScreen(selectedScreen: SelectedScreen)
    fun selectTab(browserWrapper: BrowserWrapper, tab: TabInfo)
    fun closeTab(browserWrapper: BrowserWrapper, tab: TabInfo)
    fun openLazyTab(browserWrapper: BrowserWrapper)
    fun closeAllTabs(browserWrapper: BrowserWrapper)
    fun showBrowser()
}

class CardGridModelImpl(
    private val webLayerModel: WebLayerModel,
    private val appNavModel: AppNavModel
) : CardGridModel {
    override fun switchScreen(selectedScreen: SelectedScreen) {
        when (selectedScreen) {
            SelectedScreen.REGULAR_TABS -> {
                appNavModel.showCardGrid()
                webLayerModel.switchToProfile(useIncognito = false)
            }

            SelectedScreen.INCOGNITO_TABS -> {
                appNavModel.showCardGrid()
                webLayerModel.switchToProfile(useIncognito = true)
            }
        }
    }

    override fun selectTab(browserWrapper: BrowserWrapper, tab: TabInfo) {
        browserWrapper.selectTab(tab)
        showBrowser()
    }

    override fun closeTab(browserWrapper: BrowserWrapper, tab: TabInfo) {
        browserWrapper.closeTab(tab)
    }

    override fun openLazyTab(browserWrapper: BrowserWrapper) {
        browserWrapper.openLazyTab()
        showBrowser()
    }

    override fun closeAllTabs(browserWrapper: BrowserWrapper) {
        browserWrapper.closeAllTabs()
    }

    override fun showBrowser() {
        appNavModel.showBrowser()
        webLayerModel.deleteIncognitoProfileIfUnused()
    }
}
