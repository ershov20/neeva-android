package com.neeva.app.settings

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.neeva.app.R
import com.neeva.app.settings.sharedComposables.SettingsUIConstants
import com.neeva.app.settings.sharedComposables.subcomponents.SettingsButtonRow
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.userdata.NeevaUser

@OptIn(ExperimentalCoilApi::class)
@Composable
fun ProfileRow(
    primaryLabel: String?,
    secondaryLabel: String?,
    pictureURI: Uri?,
    onClick: (() -> Unit)? = null,
    modifier: Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .then(modifier)
    ) {
        ProfileImage(
            displayName = primaryLabel,
            pictureURI = pictureURI,
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            if (primaryLabel != null) {
                Text(
                    text = primaryLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (secondaryLabel != null) {
                Text(
                    text = secondaryLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (onClick != null) {
            // TODO(dan.alcantara) Use Material Icons extended library when CircleCI issues are resolved
            Image(
                painter = painterResource(R.drawable.ic_navigate_next),
                contentDescription = stringResource(R.string.navigate_to_profile),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }
    }
}

@Composable
private fun ProfileImage(displayName: String?, pictureURI: Uri?, modifier: Modifier) {
    if (pictureURI != null) {
        Image(
            painter = rememberImagePainter(
                data = pictureURI,
                builder = { crossfade(true) }
            ),
            contentDescription = null,
            modifier = modifier
        )
    } else {
        if (displayName == null || displayName.isEmpty()) {
            Icon(
                Icons.Rounded.AccountCircle,
                contentDescription = null,
                modifier = modifier,
                tint = MaterialTheme.colorScheme.primary,
            )
        } else {
            Box(
                modifier
                    .background(MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = displayName[0].uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

class ProfileRowPreviews :
    BooleanPreviewParameterProvider<ProfileRowPreviews.Params>(6) {
    data class Params(
        val darkTheme: Boolean,
        val isSignedIn: Boolean,
        val showSSOProviderAsPrimaryLabel: Boolean,
        val invalidProfileUrl: Boolean,
        val navigatable: Boolean,
        val hasDisplayName: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0],
        isSignedIn = booleanArray[1],
        showSSOProviderAsPrimaryLabel = booleanArray[2],
        invalidProfileUrl = booleanArray[3],
        navigatable = booleanArray[4],
        hasDisplayName = booleanArray[5]
    )

    @Preview("ProfileRowPreviews 1x", locale = "en")
    @Preview("ProfileRowPreviews 2x", locale = "en", fontScale = 2.0f)
    @Preview("ProfileRowPreviews RTL, 1x", locale = "he")
    @Preview("ProfileRowPreviews RTL, 2x", locale = "he", fontScale = 2.0f)
    @Composable
    fun DefaultPreview(
        @PreviewParameter(ProfileRowPreviews::class) params: Params
    ) {
        val pictureURI: Uri? = if (!params.invalidProfileUrl) {
            Uri.parse("")
        } else {
            null
        }

        val displayName: String? = if (params.hasDisplayName) {
            "Jehan Kobe Chang"
        } else {
            null
        }

        val onClick: (() -> Unit)? = if (params.navigatable) {
            {}
        } else {
            null
        }

        NeevaTheme(useDarkTheme = params.darkTheme) {
            when (params.isSignedIn) {
                true -> {
                    SettingsButtonRow(
                        title = stringResource(R.string.settings_sign_in_to_join_neeva),
                        onClick = { },
                        modifier = SettingsUIConstants
                            .rowModifier.background(MaterialTheme.colorScheme.surface)
                    )
                }
                false -> {
                    if (params.showSSOProviderAsPrimaryLabel) {
                        ProfileRow(
                            primaryLabel = getFormattedSSOProviderName(
                                NeevaUser.SSOProvider.GOOGLE
                            ),
                            secondaryLabel = "kobec@neeva.co",
                            pictureURI = pictureURI,
                            onClick = null,
                            modifier = SettingsUIConstants
                                .rowModifier.background(MaterialTheme.colorScheme.surface)
                        )
                    } else {
                        ProfileRow(
                            primaryLabel = displayName,
                            secondaryLabel = "kobec@neeva.co",
                            pictureURI = pictureURI,
                            onClick = onClick,
                            modifier = SettingsUIConstants
                                .rowModifier.background(MaterialTheme.colorScheme.surface)
                        )
                    }
                }
            }
        }
    }
}
