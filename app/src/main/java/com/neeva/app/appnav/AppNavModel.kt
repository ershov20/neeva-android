package com.neeva.app.appnav

import android.content.Intent
import android.net.Uri
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import com.neeva.app.overflowmenu.OverflowMenuItemId
import com.neeva.app.spaces.SpaceEditMode
import com.neeva.app.storage.entities.Space
import com.neeva.app.storage.entities.SpaceItem
import kotlinx.coroutines.flow.StateFlow

/** Handles navigation between different screens, both internally and to external destinations. */
interface AppNavModel {
    val currentDestination: StateFlow<NavDestination?>
    val navController: NavHostController

    fun popBackStack()

    fun openLazyTab()
    fun openUrl(url: Uri)

    // region Internal screens
    fun showBrowser(forceUserToStayInCardGrid: Boolean = true)
    fun showCardGrid()
    fun showClearBrowsingSettings()
    fun showCookieCutterSettings()
    fun showDefaultBrowserSettings(fromWelcomeScreen: Boolean)
    fun showFeedback()
    fun showHelp()
    fun showHistory()
    fun showLicenses()
    fun showLocalFeatureFlagsPane()
    fun showProfileSettings()
    fun showSettings()
    fun showSpaceDetail(spaceID: String)
    fun showEditSpaceDialog(mode: SpaceEditMode, spaceItem: SpaceItem?, space: Space?)

    fun showWelcome()
    fun showSignInFlow()
    // endregion

    // region External screens
    fun openAndroidDefaultBrowserSettings(fromWelcomeScreen: Boolean)
    fun showAdditionalLicenses()

    fun openUrlViaIntent(uri: Uri)

    /** Safely fire an Intent out. */
    fun safeStartActivityForIntent(intent: Intent)
    // endregion

    // region Dialogs
    fun showAddToSpace()
    // endregion

    /** Fires a Share Intent for the currently displayed page. */
    fun shareCurrentPage()

    /** Fires a Share Intent for the given space. */
    fun shareSpace(space: Space)

    fun onMenuItem(id: OverflowMenuItemId)
}
