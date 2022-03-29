package com.neeva.app.cardgrid

import android.net.Uri
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.TabInfo
import com.neeva.app.browsing.WebLayerModel

interface CardsPaneModel {
    fun switchScreen(selectedScreen: SelectedScreen)
    fun showBrowser()

    fun selectTab(browserWrapper: BrowserWrapper, tab: TabInfo)
    fun closeTab(browserWrapper: BrowserWrapper, tab: TabInfo)
    fun openLazyTab(browserWrapper: BrowserWrapper)
    fun closeAllTabs(browserWrapper: BrowserWrapper)

    fun selectSpace(browserWrapper: BrowserWrapper, spaceUrl: Uri)
}

class CardsPaneModelImpl(
    private val webLayerModel: WebLayerModel,
    private val appNavModel: AppNavModel
) : CardsPaneModel {
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

            SelectedScreen.SPACES -> {
                appNavModel.showCardGrid()
                webLayerModel.switchToProfile(useIncognito = false)
            }
        }
    }

    override fun showBrowser() {
        appNavModel.showBrowser()
        webLayerModel.deleteIncognitoProfileIfUnused()
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

    override fun selectSpace(browserWrapper: BrowserWrapper, spaceUrl: Uri) {
        browserWrapper.loadUrl(spaceUrl, inNewTab = true)
        showBrowser()
    }
}
