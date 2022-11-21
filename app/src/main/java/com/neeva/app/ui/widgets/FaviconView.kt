// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.widgets

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalNeevaConstants
import com.neeva.app.R
import com.neeva.app.storage.entities.Favicon.Companion.toBitmap
import com.neeva.app.ui.TwoBooleanPreviewContainer
import com.neeva.app.ui.theme.Dimensions

/**
 * Draws an icon representing a particular site.
 *
 * When determining what image to display, the [overrideDrawableId] is prioritized over the
 * [bitmap].  If both are null, then a generic icon is displayed.
 */
@Composable
fun FaviconView(
    bitmap: Bitmap?,
    drawContainer: Boolean = true,
    overrideDrawableId: Int? = null
) {
    Surface(
        color = if (drawContainer) {
            MaterialTheme.colorScheme.surface
        } else {
            Color.Transparent
        },
        tonalElevation = 1.dp,
        shape = if (drawContainer) {
            RoundedCornerShape(Dimensions.RADIUS_SMALL)
        } else {
            RectangleShape
        }
    ) {
        val sizeModifier = Modifier.size(Dimensions.SIZE_ICON_MEDIUM)
        Box(
            contentAlignment = Alignment.Center,
            modifier = if (drawContainer) {
                Modifier.padding(Dimensions.PADDING_SMALL)
            } else {
                Modifier
            }
        ) {
            when {
                overrideDrawableId != null -> {
                    Icon(
                        painter = painterResource(overrideDrawableId),
                        contentDescription = null,
                        modifier = sizeModifier,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                bitmap != null -> {
                    // Tint is set to Color.unspecified because the current use case is to show a
                    // pre-colored bitmap.
                    Icon(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = sizeModifier
                    )
                }

                else -> {
                    Icon(
                        painter = painterResource(R.drawable.ic_public_black_24),
                        contentDescription = null,
                        modifier = sizeModifier,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun FaviconViewPreviews() {
    TwoBooleanPreviewContainer { showBackground, showBitmap ->
        val bitmap = if (showBitmap) {
            Uri.parse(LocalNeevaConstants.current.appURL).toBitmap()
        } else {
            null
        }

        FaviconView(
            bitmap = bitmap,
            drawContainer = showBackground
        )
    }
}
