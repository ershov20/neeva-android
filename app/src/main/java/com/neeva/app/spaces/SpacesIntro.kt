// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.spaces

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalIsDarkTheme
import com.neeva.app.LocalSpaceStore
import com.neeva.app.R
import com.neeva.app.cardgrid.spaces.SpaceCard
import com.neeva.app.firstrun.widgets.buttons.NeevaOnboardingButton
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions

@Composable
fun SpacesIntro(
    includeSpaceCard: Boolean = false,
    dismissSheet: () -> Unit = {}
) {
    val appNavModel = LocalAppNavModel.current
    val spaceStore = LocalSpaceStore.current

    Column(
        modifier = Modifier
            .padding(horizontal = Dimensions.PADDING_LARGE)
            .verticalScroll(rememberScrollState())
    ) {
        if (includeSpaceCard) {
            SpaceCard(
                spaceId = SpaceStore.MAKER_COMMUNITY_SPACE_ID,
                spaceName = stringResource(id = R.string.community_spaces),
                isSpacePublic = true,
                onSelect = { appNavModel.showSpaceDetail(SpaceStore.MAKER_COMMUNITY_SPACE_ID) },
                itemProvider = { spaceId -> spaceStore.contentDataForSpace(spaceId) }
            )
        }
        SpacesIntro {
            appNavModel.showSignInFlow()
            dismissSheet()
        }
    }
}

@Composable
fun SpacesIntro(onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier.padding(Dimensions.PADDING_LARGE),
        verticalArrangement = Arrangement.spacedBy(Dimensions.PADDING_LARGE)
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
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = stringResource(id = R.string.space_intro_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Image(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.PADDING_MEDIUM),
            painter = painterResource(imageRes),
            contentDescription = null
        )

        NeevaOnboardingButton(
            text = stringResource(id = R.string.space_intro_cta),
            signup = true,
            onClick = onClick
        )
    }
}

@PortraitPreviews
@Composable
fun SpacesIntroPreviewLight() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        SpacesIntro {}
    }
}

@PortraitPreviews
@Composable
fun SpacesIntroPreviewDark() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        SpacesIntro {}
    }
}
