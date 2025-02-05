// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.spaces

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalIsDarkTheme
import com.neeva.app.LocalSpaceStore
import com.neeva.app.NeevaActivity
import com.neeva.app.R
import com.neeva.app.appnav.AppNavDestination
import com.neeva.app.cardgrid.spaces.SpaceCard
import com.neeva.app.firstrun.LoginReturnParams
import com.neeva.app.firstrun.widgets.buttons.CloseButton
import com.neeva.app.ui.LandscapePreviews
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.StackableButtons
import com.neeva.app.welcomeflow.WelcomeFlowActivity

@Composable
fun SpacesIntro(
    includeSpaceCard: Boolean = false,
    dismissSheet: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val localDensity = LocalDensity.current
    var viewHeight by remember(configuration.orientation) { mutableStateOf(0.dp) }
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { layoutCoordinates ->
                    // Calculate the height of view holding the content
                    viewHeight = with(localDensity) {
                        layoutCoordinates.size.height.toDp()
                    }
                }
                .verticalScroll(rememberScrollState())
        ) {
            if (!includeSpaceCard) {
                CloseButton(onClick = dismissSheet, modifier = Modifier.align(Alignment.TopStart))
            }
            SpacesIntro(
                includeSpaceCard = includeSpaceCard,
                viewHeight = viewHeight,
                modifier = Modifier.align(
                    Alignment.Center
                )
            )
        }
    }
}

/**
 * Defines a height for [SpacesIntroContent] by calculating if it can fit within the screen
 * with or without [preferredPaddingAroundImage].
 *
 * If [SpacesIntroContent] cannot fit within the screen if [preferredPaddingAroundImage] = 0,
 *      the height will be the intrinsic minimum height with 0 padding.
 * If [SpacesIntroContent] can fit within the screen,
 *      the determined height will be minimum of the screen height or height + padding.
 */
@Composable
fun SpacesIntro(
    includeSpaceCard: Boolean,
    viewHeight: Dp,
    modifier: Modifier
) {
    val appNavModel = LocalAppNavModel.current

    val preferredPaddingAroundImage = 64.dp
    val localDensity = LocalDensity.current

    var heightWithoutPadding: Dp by remember { mutableStateOf(0.dp) }
    var heightOfContent: Dp by remember { mutableStateOf(0.dp) }
    val isCalculatingContentHeightWithoutPadding = heightWithoutPadding == 0.dp
    val contentWithoutPaddingDoesNotFitOnScreen = heightWithoutPadding > viewHeight

    Column(
        modifier = modifier
            .onGloballyPositioned { layoutCoordinates ->
                // First calculate the height of content when preferredPaddingAroundImage = 0
                if (isCalculatingContentHeightWithoutPadding) {
                    heightWithoutPadding = with(localDensity) {
                        layoutCoordinates.size.height.toDp()
                    }
                }
                heightOfContent = with(localDensity) {
                    layoutCoordinates.size.height.toDp()
                }
            }
            .then(
                if (isCalculatingContentHeightWithoutPadding) {
                    // When measuring heightWithoutPadding, set the height to be the Intrinsic min
                    Modifier.height(IntrinsicSize.Min)
                } else if (contentWithoutPaddingDoesNotFitOnScreen) {
                    // if the content (even without padding) can't fit in the screen,
                    // set the content to its minimum height without padding.
                    Modifier.height(heightWithoutPadding)
                } else {
                    // The content can fit within the screen and adding more padding is possible.

                    // If the screenHeight is really big, we would only want the height of the
                    // content to be heightWithoutPadding + preferredPaddingAroundImage * 2

                    // For screenHeight that are smaller than the content with full padding,
                    // set the height to be the height of the screen.
                    // This tells SpacesIntroContent to shrink its padding around the image
                    // as needed.
                    Modifier.height(
                        minOf(
                            viewHeight,
                            heightWithoutPadding + preferredPaddingAroundImage * 2
                        )
                    )
                }
            )

    ) {
        val paddingForCloseButton = Dimensions.PADDING_HUGE
        val closeButtonSize = Dimensions.SIZE_ICON_MEDIUM
        val contentOverlapsWithCloseButton =
            (heightOfContent + paddingForCloseButton * 2 + closeButtonSize) > viewHeight

        if (includeSpaceCard) {
            SpaceIntroCard()
        } else if (contentOverlapsWithCloseButton) {
            Spacer(modifier = Modifier.padding(paddingForCloseButton))
        }

        SpacesIntroContent(
            onClickSignIn = {
                appNavModel.showWelcomeFlow(
                    loginReturnParams = LoginReturnParams(
                        activityToReturnTo = NeevaActivity::class.java.name,
                        screenToReturnTo = AppNavDestination.BROWSER.name
                    ),
                    purpose = WelcomeFlowActivity.Companion.Purpose.SIGN_IN
                )
            },
            onClickSignUp = {
                appNavModel.showWelcomeFlow(
                    loginReturnParams = LoginReturnParams(
                        activityToReturnTo = NeevaActivity::class.java.name,
                        screenToReturnTo = AppNavDestination.BROWSER.name
                    ),
                    purpose = WelcomeFlowActivity.Companion.Purpose.SIGN_UP
                )
            },
            preferredPaddingAroundImage = if (isCalculatingContentHeightWithoutPadding) {
                // Since we are calculating the height of content when
                // preferredPaddingAroundImage = 0, set the padding to 0.
                0.dp
            } else if (contentWithoutPaddingDoesNotFitOnScreen) {
                // Since the content does not fit on screen, additional padding would not be useful.
                0.dp
            } else {
                // Since the contentWithoutPadding fits on screen, allow a preferredPadding of 64.dp
                // This padding will shrink if needed so that the content can fit on screen
                64.dp
            }
        )
    }
}

@Composable
private fun SpaceIntroCard() {
    val appNavModel = LocalAppNavModel.current
    val spaceStore = LocalSpaceStore.current
    SpaceCard(
        spaceId = SpaceStore.MAKER_COMMUNITY_SPACE_ID,
        spaceName = stringResource(id = R.string.community_spaces),
        isSpacePublic = true,
        onSelect = {
            appNavModel.showSpaceDetail(SpaceStore.MAKER_COMMUNITY_SPACE_ID)
        },
        itemProvider = { spaceId -> spaceStore.contentDataForSpace(spaceId) }
    )
}

/** This composable will adjust its padding based on the height of the container it is in. */
@Composable
fun SpacesIntroContent(
    onClickSignIn: () -> Unit,
    onClickSignUp: () -> Unit,
    preferredPaddingAroundImage: Dp,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Dimensions.PADDING_LARGE)
    ) {
        val imageRes =
            if (LocalIsDarkTheme.current) {
                R.drawable.kill_the_clutter_dark
            } else {
                R.drawable.kill_the_clutter
            }

        Text(
            text = stringResource(id = R.string.space_intro_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.size(Dimensions.PADDING_SMALL))

        Text(
            text = stringResource(id = R.string.space_intro_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        val imageHeight = 188.dp
        // Responsive layout that has Spacers that shrink and expand to a max of
        // preferredPaddingAroundImage as needed
        Column(
            Modifier
                .height(preferredPaddingAroundImage + preferredPaddingAroundImage + imageHeight)
                .weight(1f, fill = false)
        ) {
            Spacer(Modifier.weight(1f))

            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight),
                painter = painterResource(imageRes),
                contentDescription = null
            )

            Spacer(Modifier.weight(1f))
        }

        StackableButtons(
            primaryText = stringResource(id = R.string.space_intro_cta),
            onTapPrimary = onClickSignUp,
            secondaryText = stringResource(id = R.string.sign_in),
            onTapSecondary = onClickSignIn,
            modifier = Modifier.align(Alignment.End)
        )
    }
}

@PortraitPreviews
@LandscapePreviews
@Composable
fun SpacesIntroPreviewLight() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        SpacesIntroContent(
            onClickSignIn = {},
            onClickSignUp = {},
            preferredPaddingAroundImage = 64.dp
        )
    }
}

@PortraitPreviews
@Composable
fun SpacesIntroPreviewDark() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        SpacesIntroContent(
            onClickSignIn = {},
            onClickSignUp = {},
            preferredPaddingAroundImage = 64.dp
        )
    }
}
