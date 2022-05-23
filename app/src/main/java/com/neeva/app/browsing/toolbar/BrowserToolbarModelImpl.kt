package com.neeva.app.browsing.toolbar

import com.neeva.app.NeevaConstants
import com.neeva.app.ToolbarConfiguration
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.urlbar.PreviewUrlBarModel
import com.neeva.app.browsing.urlbar.URLBarModelState
import com.neeva.app.cookiecutter.PreviewCookieCutterModel
import com.neeva.app.overflowmenu.OverflowMenuItemId
import kotlinx.coroutines.flow.MutableStateFlow

class BrowserToolbarModelImpl(
    private val appNavModel: AppNavModel,
    private val browserWrapper: BrowserWrapper,
    private val toolbarConfiguration: ToolbarConfiguration,
    private val neevaConstants: NeevaConstants
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

    override fun onLoadUrl(urlBarModelState: URLBarModelState) {
        browserWrapper.loadUrl(urlBarModelState.uriToLoad)
        browserWrapper.suggestionsModel?.logSuggestionTap(
            urlBarModelState.getSuggestionType(neevaConstants),
            null
        )
    }

    override val navigationInfoFlow get() = browserWrapper.activeTabModel.navigationInfoFlow
    override val spaceStoreHasUrlFlow get() = browserWrapper.activeTabModel.isCurrentUrlInSpaceFlow
    override val displayedInfoFlow get() = browserWrapper.activeTabModel.displayedInfoFlow
    override val tabProgressFlow get() = browserWrapper.activeTabModel.progressFlow
    override val trackersFlow get() = browserWrapper.activeTabModel.trackersFlow

    override val urlBarModel = browserWrapper.urlBarModel
    override val isIncognito get() = browserWrapper.isIncognito

    override val useSingleBrowserToolbar get() = toolbarConfiguration.useSingleBrowserToolbar
    override val isUpdateAvailable get() = toolbarConfiguration.isUpdateAvailable

    override val cookieCutterModel get() = browserWrapper.cookieCutterModel
}

/** Empty [BrowserToolbarModel] that doesn't do anything. */
internal class PreviewBrowserToolbarModel(
    navigationInfo: ActiveTabModel.NavigationInfo = ActiveTabModel.NavigationInfo(),
    spaceStoreHasUrl: Boolean = false,
    override val useSingleBrowserToolbar: Boolean = false,
    override val isIncognito: Boolean = false,
    override val isUpdateAvailable: Boolean = false,
    val urlBarModelStateValue: URLBarModelState = URLBarModelState(),
    val tabProgressValue: Int = 100,
    val trackers: Int = 0,
    val displayedInfo: ActiveTabModel.DisplayedInfo = ActiveTabModel.DisplayedInfo(),
) : BrowserToolbarModel() {
    override fun goBack() {}
    override fun goForward() {}
    override fun reload() {}
    override fun share() {}
    override fun onAddToSpace() {}
    override fun onMenuItem(id: OverflowMenuItemId) {}
    override fun onTabSwitcher() {}
    override fun onLoadUrl(urlBarModelState: URLBarModelState) {}

    override val navigationInfoFlow = MutableStateFlow(navigationInfo)
    override val spaceStoreHasUrlFlow = MutableStateFlow(spaceStoreHasUrl)
    override val displayedInfoFlow get() = MutableStateFlow(displayedInfo)
    override val tabProgressFlow get() = MutableStateFlow(tabProgressValue)
    override val trackersFlow get() = MutableStateFlow(trackers)
    override val urlBarModel get() = PreviewUrlBarModel(urlBarModelStateValue)
    override val cookieCutterModel get() = PreviewCookieCutterModel()
}
