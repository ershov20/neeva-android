// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.contentfilter.ui.icon

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neeva.app.R

enum class BadgeSize(
    val height: Dp,
    val oneDigitWidth: Dp,
    val twoDigitWidth: Dp,
    val maxWidth: Dp
) {
    SMALL(16.dp, 16.dp, 21.dp, 27.dp),
    LARGE(24.dp, 24.dp, 32.dp, 36.dp),
}

@Composable
fun NumberBadge(
    number: Int,
    maxNumber: Int = 99,
    badgeSize: BadgeSize = BadgeSize.SMALL,
    modifier: Modifier = Modifier
) {
    if (number > 0) {
        BadgeImage(number, maxNumber, badgeSize, modifier)
    }
}

@Composable
private fun BadgeImage(number: Int, maxNumber: Int, badgeSize: BadgeSize, modifier: Modifier) {
    val text = if (number in 0..maxNumber) {
        number.toString()
    } else {
        stringResource(id = R.string.content_filter_too_many_items, maxNumber)
    }

    val height = badgeSize.height
    val width = when {
        text.length <= 1 -> badgeSize.oneDigitWidth
        text.length == 2 -> badgeSize.twoDigitWidth
        else -> badgeSize.maxWidth
    }

    // Draw a bitmap image that is proportionally consistent with the given height and width.
    val bitmapHeight = 128
    val bitmapWidth = (bitmapHeight * (width / height)).toInt()

    val bitmap =
        BadgeBitmap(
            text, bitmapWidth, bitmapHeight,
            MaterialTheme.colorScheme.primary.toArgb(),
            MaterialTheme.colorScheme.onPrimary.toArgb()
        )
            .asImageBitmap()

    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = modifier
            .then(Modifier.size(width = width, height = height))
            .clip(CircleShape),
        contentScale = ContentScale.FillBounds
    )
}

private fun BadgeBitmap(
    text: String,
    width: Int,
    height: Int,
    backgroundColor: Int,
    textColor: Int
): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(backgroundColor)

    // TODO(kobec): find a way to get Paint() to use Material3 text (https://github.com/neevaco/neeva-android/issues/612)
    val textPaint = Paint()
    textPaint.textAlign = Paint.Align.CENTER
    textPaint.color = textColor
    textPaint.textSize = height * 0.6f

    val xPos = canvas.width / 2.0f
    val yPos = (canvas.height - textPaint.descent() - textPaint.ascent()) / 2.0f
    canvas.drawText(text, xPos, yPos, textPaint)

    return bitmap
}
