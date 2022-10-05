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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalActivityStarter
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
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimensions.PADDING_LARGE, vertical = Dimensions.PADDING_SMALL)
    ) {
        val activityStarter = LocalActivityStarter.current
        SocialShareButton(
            name = stringResource(id = R.string.twitter),
            iconResourceID = R.drawable.twitter_logo_blue
        ) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("http://twitter.com/share?url=$spaceURL")
            )
            onTogglePublic()
            activityStarter.safeStartActivityForIntent(intent)
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
            activityStarter.safeStartActivityForIntent(intent)
        }

        SocialShareButton(
            name = stringResource(id = R.string.copy_link),
            iconResourceID = R.drawable.ic_link,
            tint = MaterialTheme.colorScheme.primary,
            onClick = {
                onTogglePublic()
                onCopyLink()
            }
        )

        SocialShareButton(
            name = stringResource(id = R.string.more),
            icon = Icons.Outlined.Share,
            tint = MaterialTheme.colorScheme.primary,
            onClick = {
                onTogglePublic()
                onMore()
            }
        )
    }
}

@Composable
fun SocialShareButton(
    name: String,
    iconResourceID: Int? = null,
    icon: ImageVector? = null,
    tint: Color? = null,
    onClick: (() -> Unit)? = null,
) {
    val buttonSize = 56.dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(width = buttonSize)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = CircleShape,
            modifier = Modifier
                .size(buttonSize)
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
                    .padding(Dimensions.PADDING_LARGE)
            ) {
                when {
                    icon != null -> Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tint ?: LocalContentColor.current,
                        modifier = Modifier.size(Dimensions.SIZE_ICON_MEDIUM)
                    )

                    iconResourceID != null ->
                        Image(
                            painterResource(id = iconResourceID),
                            contentDescription = null,
                            colorFilter = tint?.let { ColorFilter.tint(it) },
                            modifier = Modifier.size(Dimensions.SIZE_ICON_MEDIUM)
                        )
                    else -> {}
                }
            }
        }

        Text(
            text = name,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
    }
}
