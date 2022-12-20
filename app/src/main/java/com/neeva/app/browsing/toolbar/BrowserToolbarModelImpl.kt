// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing.toolbar

import android.net.Uri
import com.neeva.app.NeevaConstants
import com.neeva.app.ToolbarConfiguration
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.urlbar.PreviewUrlBarModel
import com.neeva.app.browsing.urlbar.URLBarModelState
import com.neeva.app.contentfilter.PreviewContentFilterModel
import com.neeva.app.neevascope.NeevaScopeModel
import com.neeva.app.overflowmenu.OverflowMenuItemId
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.sharedprefs.SharedPrefFolder
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.ui.PopupModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BrowserToolbarModelImpl(
    private val appNavModel: AppNavModel,
    private val popupModel: PopupModel,
    private val settingsDataModel: SettingsDataModel,
    private val sharedPreferencesModel: SharedPreferencesModel,
    private val browserWrapper: BrowserWrapper,
    private val toolbarConfiguration: ToolbarConfiguration,
    private val neevaConstants: NeevaConstants
) : BrowserToolbarModel() {
    override fun goBack() = appNavModel.navigateBackOnActiveTab()
    override fun goForward() = browserWrapper.goForward()
    override fun reload() = browserWrapper.reload()
    override fun reloadAfterContentFilterAllowListUpdate() =
        browserWrapper.reloadAfterContentFilterAllowListUpdate()

    override fun showNeevaScope() = browserWrapper.showNeevaScope()
    override fun getNeevaScopeModel(): NeevaScopeModel = browserWrapper.neevaScopeModel
    override fun shouldShowNeevaScopeTooltip(): Boolean {
        val didShowAdBlockOnboarding =
            SharedPrefFolder.FirstRun.DidShowAdBlockOnboarding.get(sharedPreferencesModel)
        val enableTrackingProtection =
            browserWrapper.contentFilterModel.enableTrackingProtection.value
        val neevaScopeOnboarding =
            SharedPrefFolder.App.NeevaScopeOnboarding.get(sharedPreferencesModel)

        return neevaScopeOnboarding && popupModel.canShowPromo &&
            (!enableTrackingProtection || didShowAdBlockOnboarding)
    }

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
            type = urlBarModelState.getSuggestionType(neevaConstants)
        )
    }

    override val navigationInfoFlow get() = browserWrapper.activeTabModel.navigationInfoFlow
    override val spaceStoreHasUrlFlow get() = browserWrapper.activeTabModel.isCurrentUrlInSpaceFlow
    override val urlFlow: StateFlow<Uri> get() = browserWrapper.activeTabModel.urlFlow
    override val displayedInfoFlow get() = browserWrapper.activeTabModel.displayedInfoFlow
    override val tabProgressFlow get() = browserWrapper.activeTabModel.progressFlow
    override val trackersFlow get() = browserWrapper.activeTabModel.trackersFlow

    override val urlBarModel = browserWrapper.urlBarModel
    override val isIncognito get() = browserWrapper.isIncognito

    override val useSingleBrowserToolbar get() = toolbarConfiguration.useSingleBrowserToolbar
    override val isUpdateAvailable get() = toolbarConfiguration.isUpdateAvailable

    override val contentFilterModel get() = browserWrapper.contentFilterModel
}

/** Empty [BrowserToolbarModel] that just provides state data. */
internal class PreviewBrowserToolbarModel(
    override val isIncognito: Boolean,
    navigationInfo: ActiveTabModel.NavigationInfo = ActiveTabModel.NavigationInfo(),
    spaceStoreHasUrl: Boolean = false,
    override val useSingleBrowserToolbar: Boolean = false,
    override val isUpdateAvailable: Boolean = false,
    val urlBarModelStateValue: URLBarModelState = URLBarModelState(),
    val tabProgressValue: Int = 100,
    val trackers: Int = 0,
    val displayedInfo: ActiveTabModel.DisplayedInfo = ActiveTabModel.DisplayedInfo(),
    val url: Uri = Uri.EMPTY
) : BrowserToolbarModel() {
    override fun goBack() {}
    override fun goForward() {}
    override fun reload() {}
    override fun reloadAfterContentFilterAllowListUpdate() {}
    override fun share() {}
    override fun showNeevaScope() {}
    override fun getNeevaScopeModel(): NeevaScopeModel? { return null }
    override fun shouldShowNeevaScopeTooltip(): Boolean { return false }
    override fun onAddToSpace() {}
    override fun onMenuItem(id: OverflowMenuItemId) {}
    override fun onTabSwitcher() {}
    override fun onLoadUrl(urlBarModelState: URLBarModelState) {}

    override val navigationInfoFlow = MutableStateFlow(navigationInfo)
    override val spaceStoreHasUrlFlow = MutableStateFlow(spaceStoreHasUrl)
    override val urlFlow get() = MutableStateFlow(url)
    override val displayedInfoFlow get() = MutableStateFlow(displayedInfo)
    override val tabProgressFlow get() = MutableStateFlow(tabProgressValue)
    override val trackersFlow get() = MutableStateFlow(trackers)
    override val urlBarModel get() = PreviewUrlBarModel(urlBarModelStateValue)
    override val contentFilterModel get() = PreviewContentFilterModel()
}
