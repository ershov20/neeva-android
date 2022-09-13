// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.history

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalPopupModel
import com.neeva.app.R
import com.neeva.app.cardgrid.archived.ArchivedTabGrid
import com.neeva.app.storage.entities.TabData
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.ui.FullScreenDialogTopBar
import com.neeva.app.ui.PopupModel
import com.neeva.app.ui.theme.Dimensions

/** Specifies which type of history should be displayed to the user in the HistoryContainer. */
enum class HistorySubpage(
    @StringRes val titleId: Int,
    val icon: ImageVector
) {
    History(R.string.history, Icons.Outlined.History),
    ArchivedTabs(R.string.archived_tabs, Icons.Outlined.Inventory2);

    companion object {
        fun String?.toHistorySubpage(): HistorySubpage {
            return when (this) {
                History.name -> History
                ArchivedTabs.name -> ArchivedTabs
                else -> History
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContainer(
    faviconCache: FaviconCache,
    initialSubpage: HistorySubpage,
    onOpenUrl: (Uri) -> Unit,
    onRestoreArchivedTab: (TabData) -> Unit,
    onDeleteAllArchivedTabs: () -> Unit,
    onDeleteArchivedTab: (TabData) -> Unit
) {
    val appNavModel = LocalAppNavModel.current
    val popupModel = LocalPopupModel.current

    var selectedTab by remember {
        mutableStateOf(initialSubpage)
    }

    Scaffold(
        topBar = @Composable {
            FullScreenDialogTopBar(
                title = stringResource(selectedTab.titleId),
                onBackPressed = appNavModel::showBrowser
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                HistorySubpage.values().forEach { subpage ->
                    Tab(
                        selected = selectedTab.ordinal == subpage.ordinal,
                        onClick = { selectedTab = subpage },
                        modifier = Modifier.height(Dimensions.SIZE_TOUCH_TARGET)
                    ) {
                        Icon(
                            subpage.icon,
                            contentDescription = stringResource(subpage.titleId)
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1.0f)) {
                when (selectedTab) {
                    HistorySubpage.ArchivedTabs -> ArchivedTabGrid(
                        faviconCache = faviconCache,
                        onRestoreArchivedTab = onRestoreArchivedTab,
                        onDeleteArchivedTab = onDeleteArchivedTab,
                        onDeleteAllArchivedTabs = {
                            showClearArchivedTabsConfirmationDialog(
                                popupModel = popupModel,
                                onConfirm = onDeleteAllArchivedTabs
                            )
                        },
                    )

                    else -> HistoryUI(
                        onClearHistory = appNavModel::showClearBrowsingSettings,
                        onOpenUrl = onOpenUrl,
                        faviconCache = faviconCache
                    )
                }
            }
        }
    }
}

fun showClearArchivedTabsConfirmationDialog(popupModel: PopupModel, onConfirm: () -> Unit) {
    popupModel.showDialog {
        AlertDialog(
            title = {
                Text(stringResource(id = R.string.archived_tabs_clear_archived))
            },
            text = {
                Text(stringResource(id = R.string.archived_tabs_are_you_sure))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirm()
                        popupModel.removeDialog()
                    }
                ) {
                    // TODO(dan.alcantara): Needs to say how many tabs are being cleared, instead.
                    // Fix after the other archiving PRs have landed.
                    Text(stringResource(id = android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = popupModel::removeDialog) {
                    Text(stringResource(id = android.R.string.cancel))
                }
            },
            onDismissRequest = popupModel::removeDialog
        )
    }
}
