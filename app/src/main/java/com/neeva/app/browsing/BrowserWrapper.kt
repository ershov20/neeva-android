// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.neeva.app.ToolbarConfiguration
import com.neeva.app.browsing.findinpage.FindInPageModel
import com.neeva.app.browsing.urlbar.URLBarModel
import com.neeva.app.cookiecutter.CookieCutterModel
import com.neeva.app.neevascope.NeevaScopeModel
import com.neeva.app.storage.entities.TabData
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.suggestions.SuggestionsModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.chromium.weblayer.Browser

/**
 * Encapsulates everything that is needed to interact with a WebLayer [Browser].
 *
 * [Browser] represents everything required to interact with a set of tabs that are associated with
 * a particular [org.chromium.weblayer.Profile].  Subclasses must be careful to ensure that anything
 * done is allowed by the incognito state defined by the Profile, which means that we are explicitly
 * trying to avoid recording history or automatically firing queries & mutations via Apollo (e.g.).
 */
interface BrowserWrapper {
    val isIncognito: Boolean

    /**
     * Meant for getting Read-only State Flows.
     * Modify these StateFlows using methods in the "Active tab operations" region below.
     */
    val activeTabModel: ActiveTabModel

    val faviconCache: FaviconCache
    val findInPageModel: FindInPageModel
    val suggestionsModel: SuggestionsModel?
    val neevaScopeModel: NeevaScopeModel
    val urlBarModel: URLBarModel
    val cookieCutterModel: CookieCutterModel

    /** List of tabs ordered by how they should appear in the CardGrid. */
    val orderedTabList: StateFlow<List<TabInfo>>

    /** Tracks if the active tab needs to be reloaded due to a renderer crash. */
    val shouldDisplayCrashedTab: Flow<Boolean>

    /** Tracks whether the user needs to be kept in the CardGrid if they're on that screen. */
    val userMustStayInCardGridFlow: StateFlow<Boolean>

    /** Gets the [Fragment] created by WebLayer for this Browser. */
    fun getFragment(): Fragment?

    /** Tracks the Lifecycle of the View associated with the Browser's Fragment. */
    val fragmentViewLifecycleEventFlow: StateFlow<Lifecycle.Event>

    /** Prepares the WebLayer Browser to interface with our app. */
    fun createAndAttachBrowser(
        displaySize: Rect,
        toolbarConfiguration: StateFlow<ToolbarConfiguration>,
        fragmentAttacher: (fragment: Fragment, isIncognito: Boolean) -> Unit
    )

    /** Suspends the coroutine until the browser has finished initialization and restoration. */
    suspend fun waitUntilBrowserIsReady(): Boolean

    fun selectTab(id: String): Boolean
    fun restoreTab(tabData: TabData)
    fun startClosingTab(id: String)
    fun cancelClosingTab(id: String)
    fun closeTab(id: String)
    fun closeAllTabs()
    fun clearAllArchivedTabs()

    /**
     * Allows the user to use the URL bar and see suggestions without opening a tab until they
     * trigger a navigation.
     */
    fun openLazyTab(focusUrlBar: Boolean = true)

    /**
     * Returns true if the [Browser] is maintaining no tabs.
     *
     * @param ignoreClosingTabs If true, any tabs that are in the process of being closed are
     * treated as not existing.
     */
    fun hasNoTabs(ignoreClosingTabs: Boolean = false): Boolean

    /** Returns true if the user should be forced to go to the card grid. */
    fun userMustBeShownCardGrid(): Boolean

    /** Returns a list of cookies split by key and values. */
    fun getCookiePairs(uri: Uri, callback: (List<CookiePair>) -> Unit)

    // region: Active tab operations
    fun goBack(): GoBackResult
    fun goForward()
    fun reload()
    fun canGoBackward(): Boolean

    fun toggleViewDesktopSite()

    fun resetOverscroll(action: Int)

    fun showFindInPage()
    fun showPageInfo()

    fun isFullscreen(): Boolean
    fun exitFullscreen(): Boolean

    /**
     * Closes the active Tab if and only if it was opened via a VIEW Intent.
     * @return True if the tab was closed.
     */
    fun closeActiveTabIfOpenedViaIntent(): Boolean

    /**
     * Start a load of the given [uri].
     *
     * If the BrowserWrapper needs to redirect the user to another URI (e.g. if the user is
     * performing a search in Incognito for the first time), the load may be delayed by a network
     * call to get the updated URL.
     *
     * If "create or switch to tab" behavior is on, this will send the user to a different tab if
     * they are not actively refining the current tab's URL or query.
     *
     * @param inNewTab If false, forces the [uri] to be loaded in the same tab.
     *                 If true, forces the [uri] to be loaded in a new tab.
     *                 If unset, the load depends on whether the user is currently opening a tab
     *                 from the TabGrid and if the user has an active tab.
     * @param searchQuery Search As You Type query used to trigger the load of the given [uri].
     */
    fun loadUrl(
        uri: Uri,
        inNewTab: Boolean? = null,
        isViaIntent: Boolean = false,
        parentTabId: String? = null,
        parentSpaceId: String? = null,
        stayInApp: Boolean = true,
        searchQuery: String? = null,
        onLoadStarted: () -> Unit = {}
    ): Job

    /** Checks whether or not the BrowserWrapper wants to block loading of the given [uri]. */
    fun shouldInterceptLoad(uri: Uri) = false

    /** Returns a URI that should be loaded in place of the given [uri]. */
    suspend fun getReplacementUrl(uri: Uri) = uri

    /** Asynchronously adds or removes the active tab from the space with given [spaceID]. */
    fun modifySpace(spaceID: String, onOpenSpace: (String) -> Unit)

    /** Dismisses any transient dialogs or popups that are covering the page. */
    fun dismissTransientUi(): Boolean
    // endregion

    // region: Screenshots
    fun takeScreenshotOfActiveTab(onCompleted: () -> Unit = {})
    suspend fun restoreScreenshotOfTab(tabId: String): Bitmap?
    suspend fun allowScreenshots(allowScreenshots: Boolean)
    // endregion

    fun reregisterActiveTabIfNecessary()

    fun reloadAfterContentFilterAllowListUpdate()

    fun showNeevaScopeTooltip(): Boolean
    fun showNeevaScope()
}

class CookiePair(val key: String, val value: String)
