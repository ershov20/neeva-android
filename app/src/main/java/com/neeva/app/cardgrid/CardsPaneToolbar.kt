// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.cardgrid

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalCardsPaneModel
import com.neeva.app.LocalNeevaUser
import com.neeva.app.LocalSettingsDataModel
import com.neeva.app.R
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.overflowmenu.OverflowMenu
import com.neeva.app.overflowmenu.OverflowMenuData
import com.neeva.app.overflowmenu.OverflowMenuItemId
import com.neeva.app.overflowmenu.overflowMenuItem
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.spaces.CreateSpaceDialog
import com.neeva.app.ui.ConfirmationAlertDialog
import com.neeva.app.ui.widgets.menu.MenuSeparator

@Composable
fun CardsPaneToolbar(browserWrapper: BrowserWrapper) {
    val appNavModel = LocalAppNavModel.current
    val cardsPaneModel = LocalCardsPaneModel.current
    val settingsDataModel = LocalSettingsDataModel.current

    val requireConfirmationWhenCloseAllTabs =
        settingsDataModel.getSettingsToggleValue(SettingsToggle.REQUIRE_CONFIRMATION_ON_TAB_CLOSE)

    val showCloseAllTabsDialog = remember { mutableStateOf(false) }

    val overflowMenuData = remember(cardsPaneModel.selectedScreen.value) {
        createCardsPaneOverflowMenuData(cardsPaneModel.selectedScreen.value)
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = dimensionResource(id = R.dimen.bottom_toolbar_height))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardsPaneToolbarAddButton(
                cardsPaneModel = cardsPaneModel,
                browserWrapper = browserWrapper
            )

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                SegmentedPicker(
                    selectedScreen = cardsPaneModel.selectedScreen,
                    onSwitchScreen = cardsPaneModel::switchScreen
                )
            }

            OverflowMenu(
                overflowMenuData = overflowMenuData,
                onMenuItem = { ordinal ->
                    when (val id = OverflowMenuItemId.values()[ordinal]) {
                        OverflowMenuItemId.CLOSE_ALL_TABS -> {
                            if (requireConfirmationWhenCloseAllTabs) {
                                showCloseAllTabsDialog.value = true
                            } else {
                                cardsPaneModel.closeAllTabs(browserWrapper)
                            }
                        }

                        else -> appNavModel.onMenuItem(id)
                    }
                }
            )
        }
    }

    if (showCloseAllTabsDialog.value) {
        val dismissLambda = { showCloseAllTabsDialog.value = false }
        val titleId = if (browserWrapper.isIncognito) {
            R.string.close_all_incognito_tabs
        } else {
            R.string.close_all_regular_tabs
        }
        ConfirmationAlertDialog(
            title = stringResource(titleId),
            onDismiss = dismissLambda,
            onConfirm = {
                cardsPaneModel.closeAllTabs(browserWrapper)
                dismissLambda()
            }
        )
    }
}

@Composable
private fun CardsPaneToolbarAddButton(
    cardsPaneModel: CardsPaneModel,
    browserWrapper: BrowserWrapper
) {
    val isCreateSpaceDialogVisible = remember { mutableStateOf(false) }
    val neevaUser = LocalNeevaUser.current
    val isCreateSpaceEnabled = !neevaUser.isSignedOut()
    val isSpaces = cardsPaneModel.selectedScreen.value == SelectedScreen.SPACES

    IconButton(
        enabled = !isSpaces || isCreateSpaceEnabled,
        onClick = {
            when (cardsPaneModel.selectedScreen.value) {
                SelectedScreen.SPACES -> { isCreateSpaceDialogVisible.value = true }
                else -> cardsPaneModel.openLazyTab(browserWrapper)
            }
        }
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = when (cardsPaneModel.selectedScreen.value) {
                SelectedScreen.SPACES -> stringResource(R.string.space_create)
                else -> stringResource(R.string.create_new_tab_a11y)
            }
        )
    }

    CreateSpaceDialog(
        isDialogVisible = isCreateSpaceDialogVisible,
        promptToOpenSpace = true,
        onDismissRequested = { isCreateSpaceDialogVisible.value = false }
    )
}

private fun createCardsPaneOverflowMenuData(selectedScreen: SelectedScreen) = OverflowMenuData(
    isBadgeVisible = false,
    additionalRowItems = when (selectedScreen) {
        SelectedScreen.SPACES -> emptyList()

        else -> {
            listOf(
                overflowMenuItem(
                    id = OverflowMenuItemId.CLOSE_ALL_TABS,
                    labelId = R.string.menu_close_all_tabs,
                    icon = Icons.Outlined.Delete
                ),
                MenuSeparator
            )
        }
    }
)
