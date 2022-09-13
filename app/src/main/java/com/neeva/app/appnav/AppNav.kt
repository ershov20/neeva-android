// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.appnav

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.composable
import com.neeva.app.LocalNeevaConstants
import com.neeva.app.LocalSettingsController
import com.neeva.app.ToolbarConfiguration
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.cardgrid.CardsPane
import com.neeva.app.feedback.FeedbackView
import com.neeva.app.firstrun.signInFlowNavGraph
import com.neeva.app.history.HistoryContainer
import com.neeva.app.history.HistorySubpage.Companion.toHistorySubpage
import com.neeva.app.settings.LicensesPane
import com.neeva.app.settings.clearbrowsing.ClearBrowsingPane
import com.neeva.app.settings.cookiecutter.CookieCutterPane
import com.neeva.app.settings.cookiecutter.CookiePreferencesPane
import com.neeva.app.settings.defaultbrowser.SetDefaultAndroidBrowserPane
import com.neeva.app.settings.featureflags.FeatureFlagsPane
import com.neeva.app.settings.main.MainSettingsPane
import com.neeva.app.settings.profile.ProfileSettingsPane
import com.neeva.app.spaces.EditSpaceDialog
import com.neeva.app.spaces.SpaceDetail
import com.neeva.app.spaces.SpaceEditMode
import com.neeva.app.ui.BrowserScaffold
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AppNav(
    toolbarConfiguration: StateFlow<ToolbarConfiguration>,
    webLayerModel: WebLayerModel,
    appNavModel: AppNavModel,
    modifier: Modifier
) {
    val settingsControllerImpl = LocalSettingsController.current
    val neevaConstants = LocalNeevaConstants.current

    AnimatedNavHost(
        navController = appNavModel.navController,
        startDestination = AppNavDestination.BROWSER.route,
        enterTransition = ::enterTransitionFactory,
        exitTransition = ::exitTransitionFactory,
        popEnterTransition = ::popEnterTransitionFactory,
        popExitTransition = ::popExitTransitionFactory,
        modifier = modifier
    ) {
        composable(AppNavDestination.BROWSER.route) {
            BrowserScaffold(toolbarConfiguration, webLayerModel)
        }

        composable(AppNavDestination.SETTINGS.route) {
            MainSettingsPane(
                settingsController = settingsControllerImpl,
                neevaConstants = neevaConstants
            )
        }

        composable(AppNavDestination.PROFILE_SETTINGS.route) {
            ProfileSettingsPane(
                settingsController = settingsControllerImpl,
                neevaConstants = neevaConstants
            )
        }

        composable(AppNavDestination.CLEAR_BROWSING_SETTINGS.route) {
            ClearBrowsingPane(
                settingsController = settingsControllerImpl,
                neevaConstants = neevaConstants
            )
        }

        composable(AppNavDestination.COOKIE_CUTTER_SETTINGS.route) {
            CookieCutterPane(
                settingsController = settingsControllerImpl,
                neevaConstants = neevaConstants
            )
        }

        composable(AppNavDestination.COOKIE_PREFERENCES.route) {
            CookiePreferencesPane(settingsController = settingsControllerImpl)
        }

        composable(AppNavDestination.SET_DEFAULT_BROWSER_SETTINGS.route) {
            SetDefaultAndroidBrowserPane(settingsController = settingsControllerImpl)
        }

        composable(AppNavDestination.LOCAL_FEATURE_FLAGS_SETTINGS.route) {
            FeatureFlagsPane(settingsController = settingsControllerImpl)
        }

        composable(AppNavDestination.LICENSES.route) {
            LicensesPane(
                onShowAdditionalLicenses = appNavModel::showAdditionalLicenses,
                onClose = appNavModel::popBackStack
            )
        }

        composable(AppNavDestination.HISTORY.route.plus("/{subpage}")) { backStackEntry ->
            val subpage = backStackEntry.arguments?.getString("subpage").toHistorySubpage()

            HistoryContainer(
                faviconCache = webLayerModel.getRegularProfileFaviconCache(),
                initialSubpage = subpage,
                onOpenUrl = appNavModel::openUrlInNewTab,
                onRestoreArchivedTab = {
                    webLayerModel.restoreArchivedTab(it)
                    appNavModel.showBrowser()
                },
                onDeleteAllArchivedTabs = webLayerModel::deleteAllArchivedTabs,
                onDeleteArchivedTab = webLayerModel::deleteArchivedTab
            )
        }

        composable(AppNavDestination.CARD_GRID.route) {
            CardsPane(
                webLayerModel = webLayerModel
            )
        }

        composable(AppNavDestination.SPACE_DETAIL.route.plus("/{spaceId}")) { backStackEntry ->
            SpaceDetail(spaceID = backStackEntry.arguments?.getString("spaceId"))
        }

        composable(
            AppNavDestination.EDIT_SPACE_DIALOG.route.plus("/{mode}/{id}"),
            arguments = listOf(
                navArgument("mode") {
                    type = NavType.EnumType(type = SpaceEditMode::class.java)
                },
                navArgument("id") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            EditSpaceDialog(
                mode = (backStackEntry.arguments?.get("mode") as SpaceEditMode),
                id = backStackEntry.arguments?.getString("id")
            )
        }

        composable(AppNavDestination.FEEDBACK.route) {
            FeedbackView(
                currentURLFlow = webLayerModel.currentBrowser.activeTabModel.urlFlow
            )
        }

        signInFlowNavGraph()
    }
}
