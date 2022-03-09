package com.neeva.app.card

import com.neeva.app.appnav.AppNavModel
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.TabInfo
import com.neeva.app.browsing.WebLayerModel

interface CardGridModel {
    fun switchScreen(selectedScreen: SelectedScreen)
    fun selectTab(tab: TabInfo)
    fun openLazyTab()
    fun closeTab(tab: TabInfo)
    fun closeAllTabs()
    fun showBrowser()
}

val mockCardGridContainerModel by lazy {
    object : CardGridModel {
        override fun switchScreen(selectedScreen: SelectedScreen) {}
        override fun selectTab(tab: TabInfo) {}
        override fun openLazyTab() {}
        override fun closeTab(tab: TabInfo) {}
        override fun closeAllTabs() {}
        override fun showBrowser() {}
    }
}

class CardGridModelImpl(
    private val webLayerModel: WebLayerModel,
    private val currentBrowserWrapper: BrowserWrapper,
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

            SelectedScreen.SPACES -> TODO("Not implemented")
        }
    }

    override fun selectTab(tab: TabInfo) {
        currentBrowserWrapper.selectTab(tab)
        showBrowser()
    }

    override fun openLazyTab() {
        currentBrowserWrapper.openLazyTab()
        showBrowser()
    }

    override fun closeTab(tab: TabInfo) {
        currentBrowserWrapper.closeTab(tab)
    }

    override fun closeAllTabs() {
        currentBrowserWrapper.closeAllTabs()
    }

    override fun showBrowser() {
        appNavModel.showBrowser()
        webLayerModel.deleteIncognitoProfileIfUnused()
    }
}
