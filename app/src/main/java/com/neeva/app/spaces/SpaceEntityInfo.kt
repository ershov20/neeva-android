// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.spaces

import android.content.pm.ProviderInfo
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import com.neeva.app.LocalDomainProvider
import com.neeva.app.settings.profile.pictureUrlPainter
import com.neeva.app.storage.entities.Favicon.Companion.toBitmap
import com.neeva.app.storage.entities.SpaceEntityType
import com.neeva.app.storage.entities.SpaceItem
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.TwoBooleanPreviewContainer
import com.neeva.app.ui.theme.ColorPalette
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.FaviconView
import com.neeva.app.ui.widgets.UriDisplayView

@Composable
fun ColumnScope.SpaceEntityInfo(spaceItem: SpaceItem) {
    when (spaceItem.itemEntityType) {
        SpaceEntityType.NEWS -> NewsInfo(
            faviconURL = spaceItem.faviconURL,
            providerName = spaceItem.provider
        )

        SpaceEntityType.RECIPE -> RecipeOrProductInfo(spaceItem = spaceItem)

        SpaceEntityType.PRODUCT -> RecipeOrProductInfo(spaceItem = spaceItem)

        SpaceEntityType.RICH_ENTITY -> {}

        else -> spaceItem.url?.let { UriDisplayView(uri = it) }
    }
}

@Composable
fun ColumnScope.RecipeOrProductInfo(spaceItem: SpaceItem) {
    val domainProvider = LocalDomainProvider.current
    val faviconBitmap = spaceItem.url?.toBitmap()
    val domain = domainProvider.getRegisteredDomain(spaceItem.url)

    RecipeOrProductInfo(spaceItem = spaceItem, faviconBitmap = faviconBitmap, domain = domain)
}

@Composable
fun ColumnScope.RecipeOrProductInfo(
    spaceItem: SpaceItem,
    faviconBitmap: Bitmap?,
    domain: String?
) {
    ProviderInfo(faviconBitmap, domain)
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (spaceItem.itemEntityType == SpaceEntityType.PRODUCT) {
            Text(
                text = buildAnnotatedString {
                    spaceItem.price?.let { append("$$it") }
                    if ((spaceItem.numReviews != null || spaceItem.stars != null) &&
                        spaceItem.price != null
                    ) {
                        append(" · ")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        spaceItem.stars?.let {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint =
                if (spaceItem.itemEntityType == SpaceEntityType.RECIPE) {
                    ColorPalette.Brand.Orange
                } else {
                    ColorPalette.Brand.Red
                },
                modifier = Modifier.padding(end = Dimensions.PADDING_TINY)
            )
        }
        Text(
            text = buildAnnotatedString {
                spaceItem.stars?.let { append("%.1f ".format(it)) }
                spaceItem.numReviews?.let { append("($it)") }
                if (spaceItem.itemEntityType == SpaceEntityType.RECIPE) {
                    if ((spaceItem.numReviews != null || spaceItem.stars != null) &&
                        spaceItem.totalTime != null
                    ) {
                        append(" · ")
                    }
                    spaceItem.totalTime?.let { append(it) }
                }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

@Composable
fun ColumnScope.ProductInfo(spaceItem: SpaceItem) {
    ProviderInfo()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Star,
            contentDescription = null,
            tint = ColorPalette.Brand.Red
        )
        Text(
            text = buildAnnotatedString {
                spaceItem.price?.let { append("%.1f".format(it)) }
                spaceItem.price?.let { append(" · ") }
                spaceItem.stars?.let { append("%.1f ".format(it)) }
                append("(${spaceItem.numReviews})")
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimensions.PADDING_MEDIUM)
        )
    }
}

@Composable
fun ColumnScope.ProviderInfo(
    faviconBitmap: Bitmap?,
    domain: String?
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.PADDING_SMALL)
    ) {
        FaviconView(
            bitmap = faviconBitmap,
            drawContainer = false
        )
        Text(
            text = domain ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Dimensions.PADDING_MEDIUM)
        )
    }
}

@Composable
fun ColumnScope.NewsInfo(
    faviconURL: Uri?,
    providerName: String?
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Dimensions.PADDING_SMALL)
    ) {
        pictureUrlPainter(pictureURI = faviconURL)?.let {
            Image(
                painter = it,
                contentDescription = null,
                modifier = Modifier
                    .size(Dimensions.SIZE_ICON_SMALL)
                    .clip(RoundedCornerShape(Dimensions.PADDING_TINY))
            )
        }
        Text(
            text = providerName ?: "",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
        )
    }
}

@PortraitPreviews
@Composable
fun SpaceEntityRecipeInfoPreview() {
    TwoBooleanPreviewContainer { isStarsNull, isTotalTimeNull ->
        Column {
            RecipeOrProductInfo(
                faviconBitmap = Uri.parse("https://allrecipes.com").toBitmap(),
                domain = "allrecipes.com",
                spaceItem = SpaceItem(
                    "asjdahjfad",
                    "nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo",
                    Uri.parse("https://allrecipes.com"),
                    "Babaganoush",
                    "Who would think that eggplant can taste this good?",
                    null,
                    stars = if (isStarsNull) null else 4.62,
                    numReviews = if (isStarsNull) null else 129,
                    totalTime = if (isTotalTimeNull) null else "1 hr 23 minutes",
                    itemEntityType = SpaceEntityType.RECIPE
                )
            )
        }
    }
}

@PortraitPreviews
@Composable
fun SpaceEntityProductInfoPreview() {
    TwoBooleanPreviewContainer { isStarsNull, isPriceNull ->
        Column {
            RecipeOrProductInfo(
                faviconBitmap = Uri.parse("https://target.com").toBitmap(),
                domain = "target.com",
                spaceItem = SpaceItem(
                    "asjdahjfad",
                    "nEgvD5HST7e62eEmhf0kkxx4xnEuNHBeEXxbGcoo",
                    Uri.parse("https://tastehome.com"),
                    "Apple iPad Pro",
                    "",
                    null,
                    stars = if (isStarsNull) null else 4.83,
                    numReviews = if (isStarsNull) null else 1278,
                    price = if (isPriceNull) null else 1293.28,
                    itemEntityType = SpaceEntityType.PRODUCT
                )
            )
        }
    }
}
