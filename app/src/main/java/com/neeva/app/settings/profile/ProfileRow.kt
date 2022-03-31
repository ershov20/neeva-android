package com.neeva.app.settings.profile

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import coil.annotation.ExperimentalCoilApi
import com.neeva.app.R
import com.neeva.app.settings.sharedComposables.SettingsUIConstants
import com.neeva.app.settings.sharedComposables.getFormattedSSOProviderName
import com.neeva.app.settings.sharedComposables.subcomponents.PictureUrlPainter
import com.neeva.app.settings.sharedComposables.subcomponents.ProfileImage
import com.neeva.app.settings.sharedComposables.subcomponents.SSOImagePainter
import com.neeva.app.settings.sharedComposables.subcomponents.SettingsButtonRow
import com.neeva.app.type.SubscriptionType
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.TwoBooleanPreviewContainer
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.userdata.NeevaUserData

@Composable
fun ProfileRowContainer(
    isSignedOut: Boolean,
    showSSOProviderAsPrimaryLabel: Boolean,
    userData: NeevaUserData,
    onClick: (() -> Unit)?,
    rowModifier: Modifier
) {
    when {
        isSignedOut && onClick != null -> {
            SettingsButtonRow(
                title = stringResource(R.string.settings_sign_in_to_join_neeva),
                onClick = onClick,
                rowModifier = rowModifier
            )
        }

        showSSOProviderAsPrimaryLabel -> {
            ProfileRow(
                primaryLabel = getFormattedSSOProviderName(userData.ssoProvider),
                secondaryLabel = userData.email,
                painter = SSOImagePainter(userData.ssoProvider), circlePicture = false,
                showSingleLetterPictureIfAvailable = false,
                onClick = null,
                rowModifier = rowModifier
            )
        }

        else -> {
            ProfileRow(
                primaryLabel = userData.displayName,
                secondaryLabel = userData.email,
                painter = PictureUrlPainter(pictureURI = userData.pictureURI),
                showSingleLetterPictureIfAvailable = true,
                subscriptionType = userData.subscriptionType,
                onClick = onClick,
                rowModifier = rowModifier,

            )
        }
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun ProfileRow(
    primaryLabel: String?,
    secondaryLabel: String?,
    painter: Painter?,
    circlePicture: Boolean = true,
    showSingleLetterPictureIfAvailable: Boolean,
    subscriptionType: SubscriptionType? = null,
    onClick: (() -> Unit)? = null,
    rowModifier: Modifier,
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
            .then(rowModifier)
    ) {
        ProfileImage(
            displayName = primaryLabel,
            painter = painter, circlePicture = circlePicture,
            showSingleLetterPictureIfAvailable = showSingleLetterPictureIfAvailable
        )
        Spacer(modifier = Modifier.width(Dimensions.PADDING_LARGE))
        Column(modifier = Modifier.weight(1f)) {
            if (primaryLabel != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = primaryLabel,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(Dimensions.PADDING_SMALL))
                    SubscriptionView(subscriptionType)
                }
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

class ProfileRowPreviews {
    private fun getMockUserData(
        hasDisplayName: Boolean = true,
        invalidProfileUrl: Boolean = false,
        subscriptionType: SubscriptionType = SubscriptionType.Basic,
        ssoProvider: NeevaUser.SSOProvider = NeevaUser.SSOProvider.GOOGLE
    ): NeevaUserData {
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

        return NeevaUserData(
            id = null,
            displayName = displayName,
            email = "kobec@neeva.co",
            pictureURI = pictureURI,
            ssoProvider = ssoProvider,
            subscriptionType = subscriptionType
        )
    }

    @Preview("NotSignedIn LTR, 1x", locale = "en")
    @Preview("NotSignedIn LTR, 2x", locale = "en", fontScale = 2.0f)
    @Preview("NotSignedIn RTL, 1x", locale = "he")
    @Composable
    fun NotSignedIn() {
        LightDarkPreviewContainer {
            ProfileRowContainer(
                isSignedOut = true,
                showSSOProviderAsPrimaryLabel = false,
                userData = getMockUserData(),
                onClick = { },
                rowModifier = SettingsUIConstants
                    .rowModifier.background(MaterialTheme.colorScheme.surface)
            )
        }
    }

    @Preview("SSO provider LTR, 1x", locale = "en")
    @Preview("SSO provider LTR, 2x", locale = "en", fontScale = 2.0f)
    @Preview("SSO provider RTL, 1x", locale = "he")
    @Composable
    fun SSOProvider() {
        LightDarkPreviewContainer {
            Column {
                NeevaUser.SSOProvider.values().forEach {
                    ProfileRowContainer(
                        isSignedOut = false,
                        showSSOProviderAsPrimaryLabel = true,
                        userData = getMockUserData(ssoProvider = it),
                        onClick = { },
                        rowModifier = SettingsUIConstants
                            .rowModifier.background(MaterialTheme.colorScheme.surface)
                    )
                }
            }
        }
    }

    @Preview("Clickable LTR, 1x", locale = "en")
    @Preview("Clickable LTR, 2x", locale = "en", fontScale = 2.0f)
    @Preview("Clickable RTL, 1x", locale = "he")
    @Composable
    fun Clickable() {
        TwoBooleanPreviewContainer { invalidProfileUrl, hasDisplayName ->
            ProfileRowContainer(
                isSignedOut = false,
                showSSOProviderAsPrimaryLabel = false,
                userData = getMockUserData(hasDisplayName, invalidProfileUrl),
                onClick = { },
                rowModifier = SettingsUIConstants
                    .rowModifier.background(MaterialTheme.colorScheme.surface)
            )
        }
    }

    @Preview("NotClickable LTR, 1x", locale = "en")
    @Preview("NotClickable LTR, 2x", locale = "en", fontScale = 2.0f)
    @Preview("NotClickable RTL, 1x", locale = "he")
    @Composable
    fun NotClickable() {
        TwoBooleanPreviewContainer { invalidProfileUrl, hasDisplayName ->
            ProfileRowContainer(
                isSignedOut = false,
                showSSOProviderAsPrimaryLabel = false,
                userData = getMockUserData(hasDisplayName, invalidProfileUrl),
                onClick = null,
                rowModifier = SettingsUIConstants
                    .rowModifier.background(MaterialTheme.colorScheme.surface)
            )
        }
    }

    @Preview("Subscription LTR, 1x", locale = "en")
    @Preview("Subscription LTR, 2x", locale = "en", fontScale = 2.0f)
    @Preview("Subscription RTL, 1x", locale = "he")
    @Composable
    fun Subscription() {
        LightDarkPreviewContainer {
            Column {
                SubscriptionType.values().forEach {
                    ProfileRowContainer(
                        isSignedOut = false,
                        showSSOProviderAsPrimaryLabel = false,
                        userData = getMockUserData(invalidProfileUrl = true, subscriptionType = it),
                        onClick = { },
                        rowModifier = SettingsUIConstants
                            .rowModifier.background(MaterialTheme.colorScheme.surface)
                    )
                }
            }
        }
    }

    @Preview("NoDisplayNameOrPicture LTR, 1x", locale = "en")
    @Preview("NoDisplayNameOrPicture LTR, 2x", locale = "en", fontScale = 2.0f)
    @Preview("NoDisplayNameOrPicture RTL, 1x", locale = "he")
    @Composable
    fun NoDisplayNameOrPicture() {
        LightDarkPreviewContainer {
            ProfileRowContainer(
                isSignedOut = false,
                showSSOProviderAsPrimaryLabel = false,
                userData = getMockUserData(hasDisplayName = false, invalidProfileUrl = true),
                onClick = null,
                rowModifier = SettingsUIConstants
                    .rowModifier.background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}
