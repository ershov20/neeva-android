// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.neeva.app.R
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions

@Composable
fun StackedButtons(
    primaryLabel: String,
    onPrimaryButton: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondaryButton: () -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(
            onClick = onPrimaryButton,
            modifier = Modifier
                .defaultMinSize(minHeight = dimensionResource(R.dimen.min_touch_target_size))
                .fillMaxWidth()
        ) {
            Text(
                text = primaryLabel,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))

        TextButton(
            onClick = onSecondaryButton,
            modifier = Modifier
                .defaultMinSize(minHeight = dimensionResource(R.dimen.min_touch_target_size))
                .fillMaxWidth(),
            enabled = secondaryLabel != null
        ) {
            secondaryLabel?.let {
                Text(
                    text = secondaryLabel,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@PortraitPreviews
@Composable
fun StackedButtonsPreview() {
    LightDarkPreviewContainer {
        StackedButtons(
            primaryLabel = stringResource(R.string.switch_default_browser_title_confirm_button),
            secondaryLabel = stringResource(R.string.maybe_later),
            onPrimaryButton = {},
            onSecondaryButton = {}
        )
    }
}

@PortraitPreviews
@Composable
fun StackedButtonsPreview_NoSecondary() {
    LightDarkPreviewContainer {
        StackedButtons(
            primaryLabel = stringResource(R.string.switch_default_browser_title_confirm_button),
            secondaryLabel = null,
            onPrimaryButton = {}
        )
    }
}
