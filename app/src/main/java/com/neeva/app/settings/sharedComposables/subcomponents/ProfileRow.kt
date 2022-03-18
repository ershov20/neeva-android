package com.neeva.app.settings.sharedComposables.subcomponents

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
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.neeva.app.R
import com.neeva.app.settings.sharedComposables.SettingsUIConstants
import com.neeva.app.settings.sharedComposables.getFormattedSSOProviderName
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.TwoBooleanPreviewContainer
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
    when {
        pictureURI != null -> {
            Image(
                painter = rememberImagePainter(
                    data = pictureURI,
                    builder = { crossfade(true) }
                ),
                contentDescription = null,
                modifier = modifier
            )
        }

        displayName == null || displayName.isEmpty() -> {
            Icon(
                Icons.Rounded.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = modifier
            )
        }

        else -> {
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier.background(MaterialTheme.colorScheme.primary)
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

class ProfileRowPreviews {
    @Preview("ProfileRowPreviews LTR, 1x", locale = "en")
    @Preview("ProfileRowPreviews LTR, 2x", locale = "en", fontScale = 2.0f)
    @Preview("ProfileRowPreviews RTL, 1x", locale = "he")
    @Composable
    fun NotSignedIn() {
        LightDarkPreviewContainer {
            SettingsButtonRow(
                title = stringResource(R.string.settings_sign_in_to_join_neeva),
                onClick = { },
                modifier = SettingsUIConstants
                    .rowModifier.background(MaterialTheme.colorScheme.surface)
            )
        }
    }

    @Preview("SSO provider LTR, 1x", locale = "en")
    @Preview("SSO provider LTR, 2x", locale = "en", fontScale = 2.0f)
    @Preview("SSO provider RTL, 1x", locale = "he")
    @Composable
    fun SSOProvider() {
        OneBooleanPreviewContainer { invalidProfileUrl ->
            val pictureURI: Uri? = if (!invalidProfileUrl) {
                Uri.parse("")
            } else {
                null
            }

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
        }
    }

    @Preview("ProfileRowPreviews LTR, 1x", locale = "en")
    @Preview("ProfileRowPreviews LTR, 2x", locale = "en", fontScale = 2.0f)
    @Preview("ProfileRowPreviews RTL, 1x", locale = "he")
    @Composable
    fun Clickable() {
        TwoBooleanPreviewContainer { invalidProfileUrl, hasDisplayName ->
            val pictureURI: Uri? = if (!invalidProfileUrl) {
                Uri.parse("")
            } else {
                null
            }

            val displayName: String? = if (hasDisplayName) {
                "Jehan Kobe Chang"
            } else {
                null
            }

            ProfileRow(
                primaryLabel = displayName,
                secondaryLabel = "kobec@neeva.co",
                pictureURI = pictureURI,
                onClick = {},
                modifier = SettingsUIConstants
                    .rowModifier.background(MaterialTheme.colorScheme.surface)
            )
        }
    }

    @Preview("ProfileRowPreviews LTR, 1x", locale = "en")
    @Preview("ProfileRowPreviews LTR, 2x", locale = "en", fontScale = 2.0f)
    @Preview("ProfileRowPreviews RTL, 1x", locale = "he")
    @Composable
    fun NotClickable() {
        TwoBooleanPreviewContainer { invalidProfileUrl, hasDisplayName ->
            val pictureURI: Uri? = if (!invalidProfileUrl) {
                Uri.parse("")
            } else {
                null
            }

            val displayName: String? = if (hasDisplayName) {
                "Jehan Kobe Chang"
            } else {
                null
            }

            ProfileRow(
                primaryLabel = displayName,
                secondaryLabel = "kobec@neeva.co",
                pictureURI = pictureURI,
                onClick = null,
                modifier = SettingsUIConstants
                    .rowModifier.background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}
