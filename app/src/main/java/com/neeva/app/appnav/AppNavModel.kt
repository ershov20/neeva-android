// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.appnav

import android.content.Context
import android.net.Uri
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import com.neeva.app.overflowmenu.OverflowMenuItemId
import com.neeva.app.spaces.SpaceEditMode
import com.neeva.app.storage.entities.Space
import com.neeva.app.storage.entities.SpaceItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Handles navigation between different screens, both internally and to external destinations. */
interface AppNavModel {
    val currentDestination: StateFlow<NavDestination?>
    val navController: NavHostController

    fun popBackStack()

    fun openLazyTab(focusUrlBar: Boolean = true)
    /** Loads the given [url] in a new tab, even if a pre-existing tab with the same URL exists. */
    fun openUrlInNewTab(url: Uri, parentSpaceId: String? = null)
    fun navigateBackOnActiveTab()

    // region Internal screens
    fun showBrowser(forceUserToStayInCardGrid: Boolean = true)
    fun showCardGrid()
    fun showClearBrowsingSettings()
    fun showContentFilterSettings()
    fun showCookiePreferences()
    fun showDefaultBrowserSettings()
    fun showFeedback()
    fun showHelp()
    fun showHistory()
    fun showArchivedTabs()
    fun showLicenses()
    fun showLocalFeatureFlagsPane()
    fun showProfileSettings()
    fun showSettings()
    fun showSpaceDetail(spaceId: String)
    fun showEditSpaceDialog(mode: SpaceEditMode, spaceItem: SpaceItem?, space: Space?)
    fun showShareSpaceSheet(spaceId: String)
    fun showSignInFlow()

    fun showWelcomeFlow()
    // endregion

    // region External screens
    fun openAndroidDefaultBrowserSettings()
    fun showAdditionalLicenses()

    fun openUrlViaIntent(uri: Uri)

    // region Dialogs
    fun showAddToSpace()
    // endregion

    /** Fires a Share Intent for the currently displayed page. */
    fun shareCurrentPage()

    /** Fires a Share Intent for the given space. */
    fun shareSpace(space: Space)

    fun onMenuItem(id: OverflowMenuItemId)
}

class PreviewAppNavModel(context: Context) : AppNavModel {
    override val currentDestination: StateFlow<NavDestination?> = MutableStateFlow(null)
    override val navController: NavHostController = NavHostController(context)

    override fun popBackStack() {}
    override fun openLazyTab(focusUrlBar: Boolean) {}
    override fun openUrlInNewTab(url: Uri, parentSpaceId: String?) {}
    override fun navigateBackOnActiveTab() {}
    override fun showBrowser(forceUserToStayInCardGrid: Boolean) {}
    override fun showCardGrid() {}
    override fun showClearBrowsingSettings() {}
    override fun showContentFilterSettings() {}
    override fun showCookiePreferences() {}
    override fun showDefaultBrowserSettings() {}
    override fun showFeedback() {}
    override fun showHelp() {}
    override fun showHistory() {}
    override fun showArchivedTabs() {}
    override fun showLicenses() {}
    override fun showLocalFeatureFlagsPane() {}
    override fun showProfileSettings() {}
    override fun showSettings() {}
    override fun showSpaceDetail(spaceId: String) { }
    override fun showEditSpaceDialog(mode: SpaceEditMode, spaceItem: SpaceItem?, space: Space?) {}
    override fun showShareSpaceSheet(spaceId: String) {}
    override fun showSignInFlow() {}
    override fun showWelcomeFlow() {}
    override fun openAndroidDefaultBrowserSettings() {}
    override fun showAdditionalLicenses() {}
    override fun openUrlViaIntent(uri: Uri) {}
    override fun showAddToSpace() {}
    override fun shareCurrentPage() {}
    override fun shareSpace(space: Space) {}
    override fun onMenuItem(id: OverflowMenuItemId) {}
}
