// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.firstrun.widgets.textfields

import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.TwoBooleanPreviewContainer

@Composable
fun PasswordTextField(
    text: String,
    onTextChanged: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    val showPassword = remember { mutableStateOf(false) }

    val visualTransformation = if (showPassword.value) {
        VisualTransformation.None
    } else {
        PasswordVisualTransformation()
    }

    OnboardingTextField(
        text = text,
        onTextChanged = onTextChanged,
        label = label,
        visualTransformation = visualTransformation,
        trailingIcon = {
            IconToggleButton(
                checked = showPassword.value,
                onCheckedChange = { showPassword.value = !showPassword.value }
            ) {
                val visibilityIcon = if (showPassword.value) {
                    painterResource(id = R.drawable.ic_visibility_24)
                } else {
                    painterResource(id = R.drawable.ic_visibility_off_24)
                }
                val description = if (showPassword.value) "Hide password" else "Show password"
                Icon(
                    painter = visibilityIcon,
                    contentDescription = description,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        modifier = modifier
    )
}

@Preview("Password Preview LTR 1x scale", locale = "en")
@Preview("Password Preview LTR 2x scale", locale = "en", fontScale = 2.0f)
@Preview("Password Preview RTL 1x scale", locale = "he")
@Composable
fun OnboardingTextField_Password_Preview() {
    TwoBooleanPreviewContainer { hasText, isFocused ->
        val startingString = if (hasText) {
            stringResource(id = R.string.debug_long_string_primary)
        } else {
            ""
        }

        val password = remember { mutableStateOf(startingString) }
        val focusRequester = remember { FocusRequester() }

        val modifier = if (isFocused) {
            Modifier.focusRequester(focusRequester)
        } else {
            Modifier
        }

        PasswordTextField(
            text = password.value,
            onTextChanged = {},
            label = "Password",
            modifier = modifier
        )

        if (isFocused) {
            LaunchedEffect(Unit) {
                focusRequester.requestFocus()
            }
        }
    }
}
