// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.widgets

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.getClickableAlpha

@Composable
fun FilledButton(
    onClick: (() -> Unit)? = null,
    text: String
) {
    val isEnabled = onClick != null
    Button(
        onClick = onClick ?: {},
        enabled = isEnabled,
        shape = RoundedCornerShape(36.dp),
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(getClickableAlpha(isEnabled))
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(Dimensions.PADDING_MEDIUM)
        )
    }
}

@Preview("1x", locale = "en", fontScale = 1.0f)
@Preview("2x", locale = "en", fontScale = 2.0f)
@Composable
fun FilledButton_Preview() {
    OneBooleanPreviewContainer { useLongString ->
        FilledButton(
            onClick = {},
            text = stringResource(
                if (useLongString) {
                    R.string.debug_long_string_primary
                } else {
                    R.string.debug_short_action
                }
            )
        )
    }
}
