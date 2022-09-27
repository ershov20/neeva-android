// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.zIndex
import com.neeva.app.R
import com.neeva.app.ui.widgets.RowActionIconButton
import com.neeva.app.ui.widgets.RowActionIconParams

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenDialogTopBar(
    title: String,
    onBackPressed: () -> Unit,
    isButtonEnabled: Boolean = true,
    buttonTitle: String? = null,
    onButtonPressed: (() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge
            )
        },
        modifier = Modifier.zIndex(1.0f),
        navigationIcon = {
            RowActionIconButton(
                iconParams = RowActionIconParams(
                    onTapAction = { onBackPressed() },
                    contentDescription = stringResource(R.string.close),
                    actionType = RowActionIconParams.ActionType.BACK
                )
            )
        },
        actions = {
            if (buttonTitle != null && onButtonPressed != null) {
                TextButton(
                    onClick = { onButtonPressed() },
                    enabled = isButtonEnabled
                ) {
                    Text(
                        text = buttonTitle,
                        maxLines = 1
                    )
                }
            }
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(MaterialTheme.colorScheme.surface)
    )
}

@Preview("FullScreenDialogTopBar, 1x font scale", locale = "en")
@Preview("FullScreenDialogTopBar, 2x font scale", locale = "en", fontScale = 2.0f)
@Preview("FullScreenDialogTopBar, RTL, 1x font scale", locale = "he")
@Preview("FullScreenDialogTopBar, RTL, 2x font scale", locale = "he", fontScale = 2.0f)
@Composable
private fun FullScreenDialogTopBarPreview() {
    OneBooleanPreviewContainer { addButton ->
        FullScreenDialogTopBar(
            title = stringResource(R.string.debug_long_string_primary),
            onBackPressed = {},
            buttonTitle = stringResource(R.string.debug_short_action).takeIf { addButton },
            onButtonPressed = {}.takeIf { addButton }
        )
    }
}
