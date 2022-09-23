// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeableDefaults.AnimationSpec
import androidx.compose.material.SwipeableState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.neeva.app.Dispatchers
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.bottomsheetdialog.BottomSheetDialogStates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DialogState(
    val content: @Composable () -> Unit
)

data class SnackbarCallbacks(
    val onActionPerformed: () -> Unit = {},
    val onDismissed: () -> Unit = {}
) {
    private var shouldFireCallback: Boolean = true

    fun fireCallback(result: SnackbarResult) {
        if (!shouldFireCallback) return
        shouldFireCallback = false

        when (result) {
            SnackbarResult.ActionPerformed -> onActionPerformed()
            SnackbarResult.Dismissed -> onDismissed()
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
data class BottomSheetDialogHostState(
    // Make the dialog slide in by starting in the GONE state and animating to HALF.
    val swipeableState: SwipeableState<BottomSheetDialogStates> = SwipeableState(
        initialValue = BottomSheetDialogStates.GONE,
        animationSpec = AnimationSpec,
        confirmStateChange = { true }
    ),
    val titleResId: Int? = null,
    val content: @Composable (dismiss: () -> Unit) -> Unit = {}
)

/** Manages state that allows the app to display Snackbars and Dialogs within the Composition. */
@OptIn(ExperimentalMaterialApi::class)
class PopupModel(
    private val coroutineScope: CoroutineScope,
    private val dispatchers: Dispatchers,
    val snackbarHostState: SnackbarHostState = SnackbarHostState()
) {
    private var currentSnackbarJob: Job? = null
    private var currentSnackbarCallbacks: SnackbarCallbacks? = null

    private val _dialogState = MutableStateFlow<DialogState?>(null)
    val dialogState: StateFlow<DialogState?> get() = _dialogState

    private val _bottomSheetDialogState = MutableStateFlow<BottomSheetDialogHostState?>(null)
    val bottomSheetDialogState: StateFlow<BottomSheetDialogHostState?>
        get() = _bottomSheetDialogState

    fun dismissSnackbar() {
        currentSnackbarJob
            ?.takeIf { it.isActive }
            ?.let {
                it.cancel()
                currentSnackbarCallbacks?.fireCallback(SnackbarResult.Dismissed)
            }
    }

    fun showSnackbar(
        message: String,
        actionLabel: String? = null,
        duration: SnackbarDuration = SnackbarDuration.Short,
        onActionPerformed: () -> Unit = {},
        onDismissed: () -> Unit = {}
    ) {
        // Cancel any previously shown Snackbars instead of letting them queue up.
        dismissSnackbar()

        // Show the new Snackbar.
        val newSnackbarCallbacks = SnackbarCallbacks(onActionPerformed, onDismissed)
        currentSnackbarCallbacks = newSnackbarCallbacks
        currentSnackbarJob = coroutineScope.launch(dispatchers.main) {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                withDismissAction = duration == SnackbarDuration.Indefinite,
                duration = duration
            )

            // Make sure that the correct callbacks are being fired.
            if (isActive && currentSnackbarCallbacks == newSnackbarCallbacks) {
                currentSnackbarCallbacks?.fireCallback(result)
                currentSnackbarCallbacks = null
            }
        }
    }

    /** Allows showing a full-screen dialog with the given content. */
    fun showDialog(content: @Composable () -> Unit) {
        _dialogState.value = DialogState(content)
    }

    /**
     * Shows a dialog suitable for showing a context menu.
     *
     * For consistency with other context menus, use [com.neeva.app.ui.widgets.menu.MenuContent].
     */
    fun showContextMenu(content: @Composable (onDismissRequested: () -> Unit) -> Unit) {
        _dialogState.value = DialogState(
            content = {
                Dialog(onDismissRequest = ::removeDialog) {
                    Surface(
                        tonalElevation = 3.dp,
                        shape = RoundedCornerShape(Dimensions.RADIUS_TINY),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        content(onDismissRequested = ::removeDialog)
                    }
                }
            }
        )
    }

    /** Removes references to any dialog that is currently showing. */
    internal fun removeDialog() {
        _dialogState.value = null
    }

    /**
     * Shows a modal bottom sheet dialog with the given [content].
     *
     * The [content] may close the dialog by calling the lambda that is passed into it.
     */
    fun showBottomSheet(
        titleResId: Int? = null,
        content: @Composable (dismiss: () -> Unit) -> Unit = {}
    ) {
        _bottomSheetDialogState.value?.let {
            // If there's already a bottom sheet showing, swap out its contents for this new one.
            _bottomSheetDialogState.value = it.copy(
                titleResId = titleResId,
                content = content
            )
        } ?: run {
            _bottomSheetDialogState.value = BottomSheetDialogHostState(
                titleResId = titleResId,
                content = content
            )
        }
    }

    /**
     * Directly removes any existing bottom sheet from the Composition.
     *
     * If you want a smoothly animated removal, you should make sure your Composable calls
     * the dismiss lambda provided to it when you call showBottomSheet.
     */
    internal fun removeBottomSheet() {
        _bottomSheetDialogState.value = null
    }
}
