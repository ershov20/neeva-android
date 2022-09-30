// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.spaces.share

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalAppNavModel
import com.neeva.app.R
import com.neeva.app.ui.NeevaSwitch
import com.neeva.app.ui.theme.Dimensions

data class ShareSpaceLinkUIParams(
    val isSpacePublic: Boolean,
    val ownerDisplayName: String,
    val ownerPictureURL: Uri?,
    val spaceURL: Uri,
    val onCopyLink: () -> Unit = {},
    val onMore: () -> Unit = {},
    val onTogglePublic: (Boolean) -> Unit = {}
)

@Composable
fun ShareSpaceLinkUI(params: ShareSpaceLinkUIParams) {
    Column(Modifier.background(color = MaterialTheme.colorScheme.background)) {
        NeevaSwitch(
            primaryLabel = stringResource(id = R.string.enable_link),
            secondaryLabel = stringResource(id = R.string.enable_link_subtitle),
            isChecked = params.isSpacePublic,
            onCheckedChange = params.onTogglePublic
        )
        SocialShareRow(
            spaceURL = params.spaceURL,
            onTogglePublic = { params.onTogglePublic(true) },
            onCopyLink = params.onCopyLink,
            onMore = params.onMore
        )
    }
}

@Composable
fun SocialShareRow(
    spaceURL: Uri,
    onTogglePublic: () -> Unit,
    onCopyLink: () -> Unit,
    onMore: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier
            .padding(Dimensions.PADDING_LARGE)
            .fillMaxWidth()
    ) {
        val appNavModel = LocalAppNavModel.current
        SocialShareButton(
            name = stringResource(id = R.string.twitter),
            iconResourceID = R.drawable.twitter_logo_blue
        ) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("http://twitter.com/share?url=$spaceURL")
            )
            onTogglePublic()
            appNavModel.safeStartActivityForIntent(intent)
        }

        SocialShareButton(
            name = stringResource(id = R.string.linkedin),
            iconResourceID = R.drawable.linkedin_logo
        ) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://linkedin.com/shareArticle?mini=true&url=$spaceURL")
            )
            onTogglePublic()
            appNavModel.safeStartActivityForIntent(intent)
        }

        SocialShareButton(
            name = stringResource(id = R.string.facebook),
            iconResourceID = R.drawable.facebook_logo
        ) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.facebook.com/sharer/sharer.php?u=$spaceURL")
            )
            onTogglePublic()
            appNavModel.safeStartActivityForIntent(intent)
        }

        SocialShareButton(
            name = stringResource(id = R.string.copy_link),
            iconResourceID = R.drawable.ic_link,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
            onClick = {
                onTogglePublic()
                onCopyLink()
            }
        )

        SocialShareButton(
            name = stringResource(id = R.string.more),
            icon = Icons.Default.Share,
            onClick = {
                onTogglePublic()
                onMore()
            }
        )
    }
}

@Composable
fun RowScope.SocialShareButton(
    name: String,
    iconResourceID: Int? = null,
    colorFilter: ColorFilter? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Dimensions.PADDING_TINY),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1.0f)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = CircleShape,
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = CircleShape
                )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clickable(enabled = onClick != null, onClick = onClick ?: {})
                    .padding(Dimensions.PADDING_MEDIUM)
            ) {
                when {
                    icon != null -> Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.SIZE_ICON_SMALL)
                    )

                    iconResourceID != null -> Image(
                        painterResource(id = iconResourceID),
                        contentDescription = null,
                        modifier = Modifier.size(Dimensions.SIZE_ICON_SMALL),
                        colorFilter = colorFilter
                    )

                    else -> {}
                }
            }
        }

        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1
        )
    }
}
