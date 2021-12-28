package com.neeva.app.widgets

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp

@Composable
fun Button(
    enabled: Boolean,
    resID: Int,
    contentDescription: String,
    onClick: () -> Unit
) {
    Image(
        imageVector = ImageVector.vectorResource(id = resID),
        contentDescription = contentDescription,
        contentScale = ContentScale.Inside,
        modifier = Modifier
            .size(48.dp, 48.dp)
            .clickable(enabled) { onClick() },
        colorFilter = ColorFilter.tint(if (enabled) MaterialTheme.colors.onPrimary else Color.LightGray)
    )
}