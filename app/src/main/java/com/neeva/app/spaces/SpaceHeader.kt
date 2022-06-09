package com.neeva.app.spaces

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.settings.profile.ProfileRow
import com.neeva.app.settings.sharedComposables.subcomponents.PictureUrlPainter
import com.neeva.app.storage.entities.Space
import com.neeva.app.type.SpaceACLLevel
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.theme.Dimensions

@Composable
fun SpaceHeader(
    space: Space
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = Dimensions.PADDING_SMALL)
    ) {
        Text(
            text = space.name,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Dimensions.PADDING_LARGE,
                    vertical = Dimensions.PADDING_SMALL
                )
        )
        ProfileRow(
            primaryLabel = space.ownerName,
            secondaryLabel = null,
            painter = PictureUrlPainter(pictureURI = space.ownerPictureURL),
            showSingleLetterPictureIfAvailable = true,
            onClick = null
        )
        Text(
            text = space.description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = Int.MAX_VALUE,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Dimensions.PADDING_LARGE,
                    vertical = Dimensions.PADDING_SMALL
                )
        )
        Row(
            modifier = Modifier.padding(
                horizontal = Dimensions.PADDING_LARGE,
                vertical = Dimensions.PADDING_SMALL
            )
        ) {
            val followers = stringResource(R.string.space_detail_followers, space.numFollowers)
            Icon(
                imageVector = Icons.Default.Face,
                tint = MaterialTheme.colorScheme.onBackground,
                contentDescription = followers
            )
            Text(
                text = followers,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(start = Dimensions.PADDING_SMALL)
            )
            Spacer(modifier = Modifier.weight(1.0f))
            if (space.userACL == SpaceACLLevel.Owner) {
                val views = stringResource(R.string.space_detail_views, space.numViews)
                Icon(
                    imageVector = Icons.Default.Favorite,
                    tint = MaterialTheme.colorScheme.onBackground,
                    contentDescription = views
                )
                Text(
                    text = views,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(start = Dimensions.PADDING_SMALL)
                )
            }
        }
    }
}

@Preview
@Composable
fun SpaceHeaderPreview() {
    OneBooleanPreviewContainer { isOwner ->
        SpaceHeader(
            Space(
                id = "c5rgtmtdv9enb8j1gv60",
                name = "Facebook Papers",
                description = "Facebook knows, in acute detail, that its platforms are riddled" +
                    "with flaws but hasn’t fixed them. - WSJ",
                lastModifiedTs = "2022-02-10T22:08:01Z",
                thumbnail = null,
                resultCount = 1,
                isDefaultSpace = true,
                isShared = true,
                isPublic = false,
                userACL = if (isOwner) SpaceACLLevel.Owner else SpaceACLLevel.PublicView,
                ownerName = "Scott Galloway",
                ownerPictureURL = null,
                numViews = 100,
                numFollowers = 25
            )
        )
    }
}
