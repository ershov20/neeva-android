package com.neeva.app.appnav

import android.content.Intent
import android.net.Uri
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import com.neeva.app.overflowmenu.OverflowMenuItemId
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
    fun showSettings()
    fun showProfileSettings()
    fun showClearBrowsingSettings()
    fun showDefaultBrowserSettings()
    fun showLocalFeatureFlagsPane()
    fun showHistory()
    fun showFeedback()
    fun showHelp()

    fun showSignUpLanding()
    fun showSignUpWithOther()
    fun showSignIn()
    // endregion

    // region External screens
    fun openAndroidDefaultBrowserSettings()

    fun openUrlViaIntent(uri: Uri)

    /** Safely fire an Intent out. */
    fun safeStartActivityForIntent(intent: Intent)
    // endregion

    // region Dialogs
    fun showAddToSpace()
    // endregion

    /** Fires a Share Intent for the currently displayed page. */
    fun shareCurrentPage()

    fun onMenuItem(id: OverflowMenuItemId)

    fun debugOpenManyTabs(numTabs: Int = 50)
}
