// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.neeva.app.appnav.AppNav
import com.neeva.app.appnav.AppNavModel
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.browsing.toolbar.BrowserToolbarModel
import com.neeva.app.cardgrid.CardsPaneModel
import com.neeva.app.feedback.FeedbackViewModel
import com.neeva.app.firstrun.FirstRunModel
import com.neeva.app.history.HistoryManager
import com.neeva.app.logging.ClientLogger
import com.neeva.app.publicsuffixlist.DomainProvider
import com.neeva.app.settings.SettingsController
import com.neeva.app.settings.SettingsDataModel
import com.neeva.app.sharedprefs.SharedPreferencesModel
import com.neeva.app.spaces.SpaceStore
import com.neeva.app.ui.DialogState
import com.neeva.app.ui.PopupModel
import com.neeva.app.ui.widgets.bottomsheetdialog.BottomSheetDialogHost
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.zeroquery.RegularProfileZeroQueryViewModel
import kotlinx.coroutines.flow.StateFlow

val LocalAppNavModel = compositionLocalOf<AppNavModel> { error("Not set") }
val LocalBrowserToolbarModel = compositionLocalOf<BrowserToolbarModel> { error("Not set") }
val LocalBrowserWrapper = compositionLocalOf<BrowserWrapper> { error("Not set") }
val LocalCardsPaneModel = compositionLocalOf<CardsPaneModel> { error("Not set") }
val LocalChromiumVersion = compositionLocalOf { "" }
val LocalClientLogger = compositionLocalOf<ClientLogger> { error("Not set") }
val LocalDispatchers = compositionLocalOf<Dispatchers> { error("Not set") }
val LocalDomainProvider = compositionLocalOf<DomainProvider> { error("Not set") }
val LocalFeedbackViewModel = compositionLocalOf<FeedbackViewModel> { error("Not set") }
val LocalFirstRunModel = compositionLocalOf<FirstRunModel> { error("Not set") }
val LocalHistoryManager = compositionLocalOf<HistoryManager> { error("Not set") }
val LocalIsDarkTheme = compositionLocalOf { false }
val LocalNavHostController = compositionLocalOf<NavHostController> { error("Not set") }
val LocalNeevaConstants = compositionLocalOf<NeevaConstants> { error("Not set") }
val LocalNeevaUser = compositionLocalOf<NeevaUser> { error("Not set") }
val LocalPopupModel = compositionLocalOf<PopupModel> { error("Not set") }
val LocalRegularProfileZeroQueryViewModel = compositionLocalOf<RegularProfileZeroQueryViewModel> {
    error("Not set")
}
val LocalSettingsController = compositionLocalOf<SettingsController> { error("Not set") }
val LocalSettingsDataModel = compositionLocalOf<SettingsDataModel> { error("Not set") }
val LocalSharedPreferencesModel = compositionLocalOf<SharedPreferencesModel> { error("Not set") }
val LocalSpaceStore = compositionLocalOf<SpaceStore> { error("Not set") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityUI(
    toolbarConfiguration: StateFlow<ToolbarConfiguration>,
    webLayerModel: WebLayerModel
) {
    val appNavModel = LocalAppNavModel.current
    val popupModel = LocalPopupModel.current

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = popupModel.snackbarHostState)
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            AppNav(
                toolbarConfiguration = toolbarConfiguration,
                webLayerModel = webLayerModel,
                appNavModel = appNavModel,
                modifier = Modifier.fillMaxSize()
            )

            BottomSheetDialogHost()
        }
    }

    val dialogState: DialogState? by popupModel.dialogState.collectAsState()
    dialogState?.let { it.content() }
}
