package com.neeva.app.spaces

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalNeevaConstants
import com.neeva.app.LocalPopupModel
import com.neeva.app.LocalSpaceStore
import com.neeva.app.R
import com.neeva.app.settings.profile.ProfileImage
import com.neeva.app.settings.profile.pictureUrlPainter
import com.neeva.app.ui.NeevaSwitch
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions

@Composable
fun ShareSpaceUIContainer(spaceID: String) {
    val spaceStore = LocalSpaceStore.current
    val spaceStoreState = spaceStore.stateFlow.collectAsState()
    val space = remember(spaceStoreState.value) {
        derivedStateOf {
            spaceStore.allSpacesFlow.value.find { it.id == spaceID }
        }
    }

    val context = LocalContext.current
    val snackbarModel = LocalPopupModel.current
    val appNavModel = LocalAppNavModel.current
    val neevaConstants = LocalNeevaConstants.current
    val spaceURL = space.value?.url(neevaConstants) ?: Uri.parse(neevaConstants.appSpacesURL)

    ShareSpaceUI(
        isSpacePublic = space.value?.isPublic ?: false,
        ownerDisplayName = space.value?.ownerName ?: "",
        ownerPictureURL = space.value?.ownerPictureURL,
        spaceURL = spaceURL,
        onCopyLink = {
            val clipboard =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(space.value?.name, spaceURL.toString())
            clipboard.setPrimaryClip(clip)
            snackbarModel.showSnackbar(context.getString(R.string.copy_clipboard))
        },
        onMore = {
            space.value?.let { appNavModel.shareSpace(it) }
        },
        onTogglePublic = {
            space.value?.let { spaceStore.toggleSpacePublicACL(it.id) }
        }
    )
}

@Composable
fun ShareSpaceUI(
    isSpacePublic: Boolean,
    ownerDisplayName: String,
    ownerPictureURL: Uri?,
    spaceURL: Uri,
    onCopyLink: () -> Unit = {},
    onMore: () -> Unit = {},
    onTogglePublic: (Boolean) -> Unit = {}
) {
    Column {
        NeevaSwitch(
            primaryLabel = stringResource(id = R.string.enable_link),
            secondaryLabel = stringResource(id = R.string.enable_link_subtitle),
            isChecked = isSpacePublic,
            onCheckedChange = onTogglePublic
        )

        if (isSpacePublic) {
            Surface(
                modifier = Modifier.padding(Dimensions.PADDING_LARGE),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(Dimensions.RADIUS_LARGE)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Dimensions.PADDING_SMALL),
                    modifier = Modifier.padding(Dimensions.PADDING_LARGE)
                ) {
                    Text(
                        text = stringResource(id = R.string.shown_as_owner),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(Dimensions.RADIUS_MEDIUM)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(Dimensions.PADDING_SMALL),
                            modifier = Modifier.padding(Dimensions.PADDING_MEDIUM)
                        ) {
                            ProfileImage(
                                displayName = ownerDisplayName,
                                painter = pictureUrlPainter(pictureURI = ownerPictureURL),
                                circlePicture = true,
                                showSingleLetterPictureIfAvailable = true
                            )

                            Text(
                                text = ownerDisplayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.CenterVertically)
                            )
                        }
                    }
                }
            }

            SocialShareRow(spaceURL = spaceURL, onCopyLink = onCopyLink, onMore = onMore)
        }
    }
}

@Composable
fun SocialShareRow(
    spaceURL: Uri,
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
            name = stringResource(id = R.string.twitter_social_title),
            iconResourceID = R.drawable.twitter_logo_blue
        ) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("http://twitter.com/share?url=$spaceURL")
            )
            appNavModel.safeStartActivityForIntent(intent)
        }

        SocialShareButton(
            name = stringResource(id = R.string.linkedin_social_title),
            iconResourceID = R.drawable.linkedin_logo
        ) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://linkedin.com/shareArticle?mini=true&url=$spaceURL")
            )
            appNavModel.safeStartActivityForIntent(intent)
        }

        SocialShareButton(
            name = stringResource(id = R.string.facebook_social_title),
            iconResourceID = R.drawable.facebook_logo
        ) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.facebook.com/sharer/sharer.php?u=$spaceURL")
            )
            appNavModel.safeStartActivityForIntent(intent)
        }

        SocialShareButton(
            name = stringResource(id = R.string.copy_link_social_title),
            iconResourceID = R.drawable.ic_link,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onBackground),
            onClick = onCopyLink
        )

        SocialShareButton(
            name = stringResource(id = R.string.more),
            icon = Icons.Default.Share,
            onClick = onMore
        )
    }
}

@Composable
fun SocialShareButton(
    name: String,
    iconResourceID: Int? = null,
    colorFilter: ColorFilter? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Dimensions.PADDING_TINY),
        horizontalAlignment = Alignment.CenterHorizontally
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
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@PortraitPreviews
@Composable
fun ShareSpaceUIPreviewLight() {
    OneBooleanPreviewContainer(useDarkTheme = false) { isSpacePublic ->
        ShareSpaceUI(
            isSpacePublic = isSpacePublic,
            ownerDisplayName = "Yusuf Ozuysal",
            ownerPictureURL = null,
            spaceURL = Uri.parse("https://neeva.com"),
        )
    }
}

@PortraitPreviews
@Composable
fun ShareSpaceUIPreviewDark() {
    OneBooleanPreviewContainer(useDarkTheme = true) { isSpacePublic ->
        ShareSpaceUI(
            isSpacePublic = isSpacePublic,
            ownerDisplayName = "Yusuf Ozuysal",
            ownerPictureURL = null,
            spaceURL = Uri.parse("https://neeva.com"),
        )
    }
}
