package com.neeva.app.settings.profile

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import coil.annotation.ExperimentalCoilApi
import com.neeva.app.R
import com.neeva.app.settings.sharedComposables.subcomponents.PictureUrlPainter
import com.neeva.app.settings.sharedComposables.subcomponents.ProfileImage
import com.neeva.app.settings.sharedComposables.subcomponents.SettingsButtonRow
import com.neeva.app.type.SubscriptionType
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.TwoBooleanPreviewContainer
import com.neeva.app.ui.layouts.BaseRowLayout
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.RowActionIconButton
import com.neeva.app.ui.widgets.RowActionIconParams
import com.neeva.app.ui.widgets.StackedText
import com.neeva.app.ui.widgets.icons.SSOImagePainter
import com.neeva.app.ui.widgets.icons.getFormattedSSOProviderName
import com.neeva.app.userdata.NeevaUser
import com.neeva.app.userdata.NeevaUserData

@Composable
fun ProfileRowContainer(
    isSignedOut: Boolean,
    showSSOProviderAsPrimaryLabel: Boolean,
    userData: NeevaUserData,
    onClick: (() -> Unit)?
) {
    when {
        isSignedOut && onClick != null -> {
            SettingsButtonRow(
                primaryLabel = stringResource(R.string.settings_sign_in_to_join_neeva),
                onClick = onClick
            )
        }

        showSSOProviderAsPrimaryLabel -> {
            ProfileRow(
                primaryLabel = getFormattedSSOProviderName(userData.ssoProvider),
                secondaryLabel = userData.email,
                painter = SSOImagePainter(userData.ssoProvider), circlePicture = false,
                showSingleLetterPictureIfAvailable = false,
                onClick = null
            )
        }

        else -> {
            ProfileRow(
                primaryLabel = userData.displayName,
                secondaryLabel = userData.email,
                painter = PictureUrlPainter(pictureURI = userData.pictureURI),
                showSingleLetterPictureIfAvailable = true,
                onClick = onClick
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
    onClick: (() -> Unit)? = null
) {
    BaseRowLayout(
        onTapRow = onClick,
        startComposable = {
            ProfileImage(
                displayName = primaryLabel,
                painter = painter, circlePicture = circlePicture,
                showSingleLetterPictureIfAvailable = showSingleLetterPictureIfAvailable
            )
        },
        endComposable = if (onClick != null) {
            {
                RowActionIconButton(
                    RowActionIconParams(
                        onTapAction = onClick,
                        actionType = RowActionIconParams.ActionType.NAVIGATE_TO_SCREEN,
                        size = Dimensions.SIZE_ICON_SMALL
                    )
                )
            }
        } else {
            null
        }
    ) {
        StackedText(primaryLabel = primaryLabel ?: "", secondaryLabel = secondaryLabel)
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
                onClick = { }
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
                        onClick = { }
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
                onClick = { }
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
                onClick = null
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
                        onClick = { }
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
                onClick = null
            )
        }
    }
}
