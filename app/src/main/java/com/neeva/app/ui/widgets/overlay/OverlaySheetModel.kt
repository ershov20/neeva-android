package com.neeva.app.ui.widgets.overlay

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf

data class OverlaySheetHostState(
    val isVisible: Boolean = false,
    val titleResId: Int? = null,
    val content: @Composable () -> Unit = {}
)

/** Controls when an Overlay Sheet shows up at the bottom of the screen. */
class OverlaySheetModel {
    internal val hostState = mutableStateOf(OverlaySheetHostState())

    fun showOverlaySheet(titleResId: Int, content: @Composable () -> Unit = {}) {
        hostState.value = OverlaySheetHostState(
            isVisible = true,
            titleResId = titleResId,
            content = content
        )
    }

    fun hideOverlaySheet() {
        hostState.value = hostState.value.copy(isVisible = false)
    }
}
