package com.neeva.app.ui

import android.content.Context
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Tries to dismiss the keyboard when the parent Composable is disposed.
 *
 * If given a [focusRequester], it will request focus on the control when the control is ready.
 */
@Composable
fun KeyboardFocusEffect(focusRequester: FocusRequester?) {
    // Requests focus on the given control when the Window containing the Composable gets focused.
    // This is adapted from a bug filed at: https://issuetracker.google.com/issues/199631318
    val windowInfo = LocalWindowInfo.current
    LaunchedEffect(windowInfo) {
        val flow: Flow<Boolean> = snapshotFlow { windowInfo.isWindowFocused }
        withContext(Dispatchers.Main) {
            flow.collect { isWindowFocused ->
                if (isWindowFocused) {
                    focusRequester?.requestFocus()
                }
            }
        }
    }

    val view = LocalView.current
    DisposableEffect(true) {
        onDispose {
            // There's a strange bug with Compose that prevents the keyboard from being dismissed
            // when a TextField loses focus.  Although we can confirm that the control has lost
            // focus, the keyboard stays visible after the Composable leaves the composition, and
            // all attempts to use LocalSoftwareKeyboardController's hide() silently fail because
            // Compose does not guarantee that hide() will actually do anything.  Asking the
            // InputMethodManager directly to hide the keyboard seems to work more often than not,
            // but I don't like not understanding why.
            (view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }
}
