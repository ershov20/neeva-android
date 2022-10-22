// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.widgets.bottomsheetdialog

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.neeva.app.LocalPopupModel

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheetDialogHost() {
    val popupModel = LocalPopupModel.current
    val bottomSheetDialogHostState by popupModel.bottomSheetDialogState.collectAsState()

    bottomSheetDialogHostState?.let { hostState ->
        BottomSheetDialog(
            state = hostState.swipeableState,
            hasHalfwayState = hostState.hasHalfwayState,
            titleResId = hostState.titleResId,
            onGone = popupModel::removeBottomSheet
        ) { dismiss ->
            hostState.content(dismiss)
        }
    }
}
