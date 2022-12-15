// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.firstrun.widgets.texts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.firstrun.FirstRunConstants
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.theme.Dimensions

@Composable
fun ToggleSignUpText(signup: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        style = FirstRunConstants.getSubtextStyle(),
        textAlign = TextAlign.Center,
        text = buildAnnotatedString {
            append(
                stringResource(
                    id = if (signup) {
                        R.string.already_have_account
                    } else {
                        R.string.dont_have_account
                    }
                )
            )

            append(" ")

            withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                append(
                    stringResource(
                        id = if (signup) {
                            R.string.sign_in
                        } else {
                            R.string.sign_up
                        }
                    )
                )
            }
        },
        modifier = modifier
            .clickable { onClick() }
            .fillMaxWidth()
            .wrapContentSize(align = Alignment.Center)
    )
}

@Preview("ToggleSignUpText LTR 1x scale", locale = "en")
@Preview("ToggleSignUpText LTR 2x scale", locale = "en", fontScale = 2.0f)
@Preview("ToggleSignUpText RTL 1x scale", locale = "he")
@Composable
fun ToggleSignUpTextPreview() {
    OneBooleanPreviewContainer { signup ->
        ToggleSignUpText(
            signup = signup,
            onClick = {},
            modifier = Modifier.padding(vertical = Dimensions.PADDING_HUGE)
        )
    }
}
