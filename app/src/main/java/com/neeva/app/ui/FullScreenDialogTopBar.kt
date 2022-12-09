// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import com.neeva.app.R
import com.neeva.app.ui.widgets.RowActionIconButton
import com.neeva.app.ui.widgets.RowActionIconParams

@Composable
fun FullScreenDialogTopBar(
    title: String,
    onBackPressed: () -> Unit
) = FullScreenDialogTopBar(
    title = title,
    onBackPressed = onBackPressed,
    isButtonEnabled = false,
    buttonTitle = null,
    buttonPainter = null,
    onButtonPressed = null,
)

@Composable
fun FullScreenDialogTopBar(
    title: String,
    onBackPressed: () -> Unit,
    isButtonEnabled: Boolean = true,
    buttonTitle: String,
    onButtonPressed: (() -> Unit)
) = FullScreenDialogTopBar(
    title = title,
    onBackPressed = onBackPressed,
    isButtonEnabled = isButtonEnabled,
    buttonTitle = buttonTitle,
    buttonPainter = null,
    onButtonPressed = onButtonPressed,
)

@Composable
fun FullScreenDialogTopBar(
    title: String,
    onBackPressed: () -> Unit,
    isButtonEnabled: Boolean = true,
    buttonPainter: Painter,
    contentDescription: String,
    onButtonPressed: (() -> Unit)
) = FullScreenDialogTopBar(
    title = title,
    onBackPressed = onBackPressed,
    isButtonEnabled = isButtonEnabled,
    buttonTitle = contentDescription,
    buttonPainter = buttonPainter,
    onButtonPressed = onButtonPressed
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullScreenDialogTopBar(
    title: String,
    onBackPressed: () -> Unit,
    isButtonEnabled: Boolean,
    buttonTitle: String?,
    buttonPainter: Painter?,
    onButtonPressed: (() -> Unit)?
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
                RowActionIconParams(
                    onTapAction = { onBackPressed() },
                    contentDescription = stringResource(R.string.toolbar_go_back),
                    actionType = RowActionIconParams.ActionType.BACK
                )
            )
        },
        actions = {
            // Runtime exceptions shouldn't be triggered if the public functions with stricter
            // argument requirements are used.
            when {
                onButtonPressed == null -> {
                    assert(buttonPainter == null && buttonTitle == null && !isButtonEnabled) {
                        "Button info provided without an onButtonPressed callback"
                    }
                }
                buttonPainter != null &&
                    buttonTitle != null -> {
                    IconButton(onClick = onButtonPressed, enabled = isButtonEnabled) {
                        Icon(
                            painter = buttonPainter,
                            contentDescription = buttonTitle,
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                buttonTitle != null -> {
                    TextButton(onClick = onButtonPressed, enabled = isButtonEnabled) {
                        Text(text = buttonTitle, maxLines = 1)
                    }
                }
                else -> throw IllegalArgumentException(
                    "If onButtonPressed callback is enabled, buttonTitle must be provided, " +
                        "even when a the button is an icon"
                )
            }
        },
        colors = TopAppBarDefaults.mediumTopAppBarColors(MaterialTheme.colorScheme.surface)
    )
}

@PortraitPreviews
@Composable
private fun FullScreenDialogTopBarPreviewNoButton() {
    FullScreenDialogTopBar(
        title = stringResource(R.string.debug_long_string_primary),
        onBackPressed = {},
    )
}

@PortraitPreviews
@Composable
private fun FullScreenDialogTopBarPreviewWithButton() {
    FullScreenDialogTopBar(
        title = stringResource(R.string.debug_long_string_primary),
        onBackPressed = {},
        buttonTitle = stringResource(R.string.debug_short_action),
        onButtonPressed = {}
    )
}

@PortraitPreviews
@Composable
private fun FullScreenDialogTopBarIconButtonPreview() {
    FullScreenDialogTopBar(
        title = stringResource(R.string.debug_long_string_primary),
        onBackPressed = {},
        isButtonEnabled = true,
        buttonPainter = painterResource(id = R.drawable.ic_send),
        buttonTitle = stringResource(R.string.debug_short_action),
        onButtonPressed = {}
    )
}
