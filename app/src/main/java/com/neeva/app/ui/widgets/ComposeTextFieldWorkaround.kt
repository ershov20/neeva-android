package com.neeva.app.ui.widgets

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import com.neeva.app.ui.KeyboardFocusEffect

/**
 * Super dirty hack to work around https://issuetracker.google.com/issues/230536793
 * Compose has a bug that prevents the keyboard from being dismissed correctly when
 * the parent of a TextField is removed.  Because the OutlinedTextField is contained
 * as part of the AlertDialog (which normally disappears all at once), we hit it.
 * Oddly, this manifests as the keyboard disappearing and then reappearing as
 * _something_ that is no longer on screen requests focus again.
 * To get around this, we start the dialog dismissal by removing the TextField from
 * the composition (which hides the keyboard), then remove the dialog itself.
 */
@Composable
fun ComposeTextFieldWorkaround(
    isDismissing: Boolean,
    onDismissRequested: () -> Unit,
    content: @Composable (FocusRequester) -> Unit
) {
    if (!isDismissing) {
        val focusRequester = remember { FocusRequester() }
        KeyboardFocusEffect(focusRequester)
        content(focusRequester)
    } else {
        LaunchedEffect(true) {
            onDismissRequested()
        }
    }
}
