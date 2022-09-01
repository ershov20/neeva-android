// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.firstrun.widgets.textfields

import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

fun View.isKeyboardOpen(): Boolean {
    val insets = ViewCompat.getRootWindowInsets(this)
    return insets?.isVisible(WindowInsetsCompat.Type.ime()) ?: false
}

/**
 Caveat: Only use this within FirstRun. Outside-package-use could slow down the main app's UI.

 Why:
 * ViewTreeObserver.OnGlobalLayoutListener is more accurate. (especially for phones with split screens)
 * but it could slow down the rest of the app's UI because OnGlobalLayoutListener is hit a lot.
 * (https://proandroiddev.com/android-11-creating-an-ime-keyboard-visibility-listener-c390a40d1ad0)
 *
 * This is not a big problem for FirstRun because its screens are temporary.
**/
@Composable
internal fun rememberIsKeyboardOpen(): State<Boolean> {
    val view = LocalView.current
    // source: https://stackoverflow.com/questions/68389802/how-to-clear-textfield-focus-when-closing-the-keyboard-and-prevent-two-back-pres
    return produceState(initialValue = view.isKeyboardOpen()) {
        val viewTreeObserver = view.viewTreeObserver
        val listener = ViewTreeObserver.OnGlobalLayoutListener { value = view.isKeyboardOpen() }
        viewTreeObserver.addOnGlobalLayoutListener(listener)

        awaitDispose { viewTreeObserver.removeOnGlobalLayoutListener(listener) }
    }
}

/**
 * When a textfield (or the container of the textfield) has focus, use this modifier to
 * clear focus when the keyboard dismisses.
 *
 * Use within FirstRun only because it uses rememberIsKeyboardOpen().
 */
internal fun Modifier.clearFocusOnKeyboardDismiss(): Modifier = composed {
    var isFocused by remember { mutableStateOf(false) }
    var keyboardAppearedSinceLastFocused by remember { mutableStateOf(false) }

    if (isFocused) {
        val isKeyboardOpen by rememberIsKeyboardOpen()

        val focusManager = LocalFocusManager.current

        // if this composable is deleted or recomposed, the old LaunchedEffect will cancel.
        // On recomposition or isKeyboardOpen changing, the LaunchedEffect will start running.
        LaunchedEffect(isKeyboardOpen) {
            when {
                // assumes that we are already focused on something because the keyboard is open.
                isKeyboardOpen -> keyboardAppearedSinceLastFocused = true

                // keyboard was open (but no longer is):
                keyboardAppearedSinceLastFocused -> focusManager.clearFocus()
            }
        }
    }

    onFocusEvent {
        // on any Focus Event, update our isFocused variable to match the Modifier's.
        if (isFocused != it.isFocused) {
            isFocused = it.isFocused
            if (isFocused) {
                // everytime we get Focus, initialize keyboardAppearedSinceLastFocused to false.
                keyboardAppearedSinceLastFocused = false
                // the keyboard will eventually appear after gaining focus.
            }
        }
    }
}
