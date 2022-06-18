package com.neeva.app.ui.widgets.bottomsheetdialog

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.neeva.app.LocalEnvironment

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BottomSheetDialogHost() {
    val popupModel = LocalEnvironment.current.popupModel
    val bottomSheetDialogHostState by popupModel.bottomSheetDialogState.collectAsState()

    bottomSheetDialogHostState?.let { hostState ->
        BottomSheetDialog(
            state = hostState.swipeableState,
            titleResId = hostState.titleResId,
            onGone = popupModel::removeBottomSheet
        ) { dismiss ->
            hostState.content(dismiss)
        }
    }
}
