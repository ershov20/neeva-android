package com.neeva.app.ui.widgets.overlay

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import com.neeva.app.ui.AnimationConstants

@Composable
fun OverlaySheetHost(
    hostState: MutableState<OverlaySheetHostState>,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = hostState.value.isVisible,
        enter = slideInVertically(
            animationSpec = tween(AnimationConstants.ANIMATION_DURATION_MS),
            initialOffsetY = { it }
        ),
        exit = slideOutVertically(
            animationSpec = tween(AnimationConstants.ANIMATION_DURATION_MS),
            targetOffsetY = { it }
        ) + fadeOut(animationSpec = tween(AnimationConstants.ANIMATION_DURATION_MS))
    ) {
        OverlaySheet(
            onDismiss = onDismiss,
            titleResId = hostState.value.titleResId
        ) {
            hostState.value.content()
        }
    }

    BackHandler(enabled = hostState.value.isVisible) {
        onDismiss()
    }
}
