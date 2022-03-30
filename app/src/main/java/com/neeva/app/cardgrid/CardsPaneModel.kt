package com.neeva.app.cardgrid

import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.neeva.app.NeevaConstants
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.TabInfo
import com.neeva.app.browsing.WebLayerModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

interface CardsPaneModel {
    val selectedScreen: MutableState<SelectedScreen>
    val previousScreen: MutableState<SelectedScreen?>

    fun switchScreen(newSelectedScreen: SelectedScreen)
    fun showBrowser()
    fun showSpacesWebsite()

    fun selectTab(browserWrapper: BrowserWrapper, tab: TabInfo)
    fun closeTab(browserWrapper: BrowserWrapper, tab: TabInfo)
    fun openLazyTab(browserWrapper: BrowserWrapper)
    fun closeAllTabs(browserWrapper: BrowserWrapper)

    fun selectSpace(browserWrapper: BrowserWrapper, spaceUrl: Uri)
}

class CardsPaneModelImpl(
    private val webLayerModel: WebLayerModel,
    private val appNavModel: AppNavModel,
    coroutineScope: CoroutineScope
) : CardsPaneModel {
    // Keep track of what screen is currently being viewed by the user.
    override val selectedScreen: MutableState<SelectedScreen> = mutableStateOf(
        if (webLayerModel.currentBrowser.isIncognito) {
            SelectedScreen.INCOGNITO_TABS
        } else {
            SelectedScreen.REGULAR_TABS
        }
    )

    override val previousScreen: MutableState<SelectedScreen?> = mutableStateOf(null)

    init {
        coroutineScope.launch {
            webLayerModel.currentBrowserFlow.collectLatest {
                if (it.isIncognito) {
                    when (selectedScreen.value) {
                        SelectedScreen.INCOGNITO_TABS -> {
                            // Do nothing.
                        }

                        SelectedScreen.REGULAR_TABS, SelectedScreen.SPACES -> {
                            updateSelectedScreen(SelectedScreen.INCOGNITO_TABS)
                        }
                    }
                } else {
                    when (selectedScreen.value) {
                        SelectedScreen.INCOGNITO_TABS -> {
                            updateSelectedScreen(SelectedScreen.REGULAR_TABS)
                        }

                        SelectedScreen.REGULAR_TABS, SelectedScreen.SPACES -> {
                            // Do nothing.
                        }
                    }
                }
            }
        }
    }

    private fun updateSelectedScreen(newSelectedScreen: SelectedScreen) {
        previousScreen.value = selectedScreen.value
        selectedScreen.value = newSelectedScreen
    }

    override fun switchScreen(newSelectedScreen: SelectedScreen) {
        updateSelectedScreen(newSelectedScreen)

        when (newSelectedScreen) {
            SelectedScreen.REGULAR_TABS -> {
                webLayerModel.switchToProfile(useIncognito = false)
            }

            SelectedScreen.INCOGNITO_TABS -> {
                webLayerModel.switchToProfile(useIncognito = true)
            }

            SelectedScreen.SPACES -> {
                webLayerModel.switchToProfile(useIncognito = false)
            }
        }
    }

    override fun showBrowser() {
        appNavModel.showBrowser()
        webLayerModel.deleteIncognitoProfileIfUnused()
    }

    override fun showSpacesWebsite() {
        appNavModel.showBrowser()
        webLayerModel.currentBrowser.loadUrl(Uri.parse(NeevaConstants.appSpacesURL))
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
