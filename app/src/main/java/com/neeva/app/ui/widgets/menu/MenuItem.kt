// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.widgets.menu

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.neeva.app.R
import com.neeva.app.ui.theme.Dimensions

interface MenuItem {
    @Composable fun Composed(onClick: (id: Int) -> Unit)
}

data class MenuHeader(
    internal val label: String? = null,
    internal val secondaryLabel: String? = null,
    internal val imageUrl: Uri? = null
) : MenuItem {
    init {
        assert(label != null || secondaryLabel != null)
    }

    @Composable
    override fun Composed(onClick: (id: Int) -> Unit) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = dimensionResource(R.dimen.min_touch_target_size))
                .padding(vertical = Dimensions.PADDING_SMALL)
        ) {
            imageUrl?.let {
                Spacer(modifier = Modifier.width(Dimensions.PADDING_LARGE))

                AsyncImage(
                    model = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(32.dp)
                        .semantics { testTag = "MenuHeaderImage" }
                )
            }

            Spacer(modifier = Modifier.width(Dimensions.PADDING_LARGE))

            Column(modifier = Modifier.weight(1.0f)) {
                label?.let {
                    Text(
                        text = label,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { testTag = "MenuHeaderLabel" }
                    )
                }

                secondaryLabel?.let {
                    Text(
                        text = secondaryLabel,
                        maxLines = 5,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.width(Dimensions.PADDING_LARGE))
        }
    }
}

object MenuSeparator : MenuItem {
    @Composable
    override fun Composed(onClick: (id: Int) -> Unit) {
        Spacer(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .height(1.dp)
                .fillMaxWidth()
        )
    }
}

data class MenuAction(
    val id: Int,
    @StringRes internal val labelId: Int = id,
    internal val icon: ImageVector? = null,
    @DrawableRes internal val imageResourceID: Int? = null
) : MenuItem {
    @Composable
    override fun Composed(onClick: (id: Int) -> Unit) {
        val label = stringResource(id = labelId)
        val iconPainter =
            icon?.let { rememberVectorPainter(image = icon) }
                ?: imageResourceID?.let { painterResource(id = imageResourceID) }

        DropdownMenuItem(
            text = {
                Text(
                    modifier = Modifier.padding(horizontal = Dimensions.PADDING_SMALL),
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1
                )
            },
            leadingIcon = iconPainter?.let {
                {
                    Icon(
                        painter = it,
                        contentDescription = null
                    )
                }
            },
            onClick = { onClick(id) }
        )
    }
}
