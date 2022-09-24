// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.package com.neeva.app.spaces
package com.neeva.app.spaces.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.apollographql.apollo3.api.Optional
import com.neeva.app.AddSpaceSoloACLsMutation
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalNeevaConstants
import com.neeva.app.LocalPopupModel
import com.neeva.app.LocalSettingsDataModel
import com.neeva.app.LocalSpaceStore
import com.neeva.app.R
import com.neeva.app.settings.SettingsToggle
import com.neeva.app.type.AddSpaceSoloACLsInput
import com.neeva.app.type.SpaceEmailACL
import com.neeva.app.ui.NeevaThemePreviewContainer
import com.neeva.app.ui.PortraitPreviews

@Composable
fun ShareSpaceSheet(spaceId: String, dismissBottomSheet: () -> Unit) {
    val spaceStore = LocalSpaceStore.current
    val spaceStoreState = spaceStore.stateFlow.collectAsState()
    val space = remember(spaceStoreState.value) {
        derivedStateOf {
            spaceStore.allSpacesFlow.value.find { it.id == spaceId }
        }
    }

    val context = LocalContext.current
    val popupModel = LocalPopupModel.current
    val appNavModel = LocalAppNavModel.current
    val neevaConstants = LocalNeevaConstants.current
    val spaceURL = space.value?.url(neevaConstants) ?: Uri.parse(neevaConstants.appSpacesURL)

    val shareSpaceLinkUIParams = ShareSpaceLinkUIParams(
        isSpacePublic = space.value?.isPublic ?: false,
        ownerDisplayName = space.value?.ownerName ?: "",
        ownerPictureURL = space.value?.ownerPictureURL,
        spaceURL = spaceURL,
        onCopyLink = {
            val clipboard =
                context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(space.value?.name, spaceURL.toString())
            clipboard.setPrimaryClip(clip)
            popupModel.showSnackbar(context.getString(R.string.copied_to_clipboard))
        },
        onMore = {
            space.value?.let { appNavModel.shareSpace(it) }
        },
        onTogglePublic = { newIsPublicValue ->
            space.value?.let { spaceStore.setSpacePublicACL(it.id, newIsPublicValue) }
        }
    )

    val shareSpaceConfirmationMessage =
        stringResource(id = R.string.share_space_confirmation_message)

    val enableInviteUser = LocalSettingsDataModel.current
        .getSettingsToggleValue(SettingsToggle.DEBUG_ENABLE_INVITE_USER_TO_SPACE)

    ShareSpaceSheet(
        shareSpaceLinkUIParams = shareSpaceLinkUIParams,
        enableInviteUser = enableInviteUser,
        isInvitingUser = false,
        addUserToSpace = { shareWith, note ->
            AddSpaceSoloACLsMutation(
                AddSpaceSoloACLsInput(
                    id = space.value?.id.let { Optional.presentIfNotNull(it) },
                    shareWith = listOf(shareWith).let { Optional.presentIfNotNull(it) },
                    note = note?.let { Optional.presentIfNotNull(it) }
                        ?: Optional.Absent
                )
            )
        },
        onFinishInvitation = {
            dismissBottomSheet()
            popupModel.showSnackbar(shareSpaceConfirmationMessage)
        }
    )
}

@Composable
fun ShareSpaceSheet(
    shareSpaceLinkUIParams: ShareSpaceLinkUIParams,
    enableInviteUser: Boolean,
    isInvitingUser: Boolean,
    addUserToSpace: (shareWith: SpaceEmailACL, note: String?) -> Unit,
    onFinishInvitation: () -> Unit
) {
    Column {
        ShareSpaceLinkUI(shareSpaceLinkUIParams)

        Divider(color = MaterialTheme.colorScheme.surfaceVariant)

        if (enableInviteUser) {
            InviteUserUI(
                isInvitingUser = isInvitingUser,
                addUserToSpace = addUserToSpace,
                onFinishInvitation = onFinishInvitation
            )
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
    }
}

@PortraitPreviews
@Composable
fun ShareSpaceSheetPreview_NonPublicSpace_Invite_UserLight() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        val shareSpaceLinkUIParams = ShareSpaceLinkUIParams(
            isSpacePublic = false,
            ownerDisplayName = "Yusuf Ozuysal",
            ownerPictureURL = null,
            spaceURL = Uri.parse("https://neeva.com")
        )
        ShareSpaceSheet(
            shareSpaceLinkUIParams = shareSpaceLinkUIParams,
            enableInviteUser = true,
            isInvitingUser = true,
            addUserToSpace = { _, _ -> },
            onFinishInvitation = {}
        )
    }
}

@PortraitPreviews
@Composable
fun ShareSpaceSheetPreview_NonPublicSpace_InviteUser_Dark() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        val shareSpaceLinkUIParams = ShareSpaceLinkUIParams(
            isSpacePublic = false,
            ownerDisplayName = "Yusuf Ozuysal",
            ownerPictureURL = null,
            spaceURL = Uri.parse("https://neeva.com")
        )
        ShareSpaceSheet(
            shareSpaceLinkUIParams = shareSpaceLinkUIParams,
            enableInviteUser = true,
            isInvitingUser = true,
            addUserToSpace = { _, _ -> },
            onFinishInvitation = {}
        )
    }
}

@PortraitPreviews
@Composable
fun ShareSpaceSheetPreview_PublicSpace_NoInviteUser_Light() {
    NeevaThemePreviewContainer(useDarkTheme = false) {
        val shareSpaceLinkUIParams = ShareSpaceLinkUIParams(
            isSpacePublic = true,
            ownerDisplayName = "Yusuf Ozuysal",
            ownerPictureURL = null,
            spaceURL = Uri.parse("https://neeva.com")
        )
        ShareSpaceSheet(
            shareSpaceLinkUIParams = shareSpaceLinkUIParams,
            enableInviteUser = true,
            isInvitingUser = false,
            addUserToSpace = { _, _ -> },
            onFinishInvitation = {}
        )
    }
}

@PortraitPreviews
@Composable
fun ShareSpaceSheetPreview_PublicSpace_NoInviteUser_Dark() {
    NeevaThemePreviewContainer(useDarkTheme = true) {
        val shareSpaceLinkUIParams = ShareSpaceLinkUIParams(
            isSpacePublic = true,
            ownerDisplayName = "Yusuf Ozuysal",
            ownerPictureURL = null,
            spaceURL = Uri.parse("https://neeva.com")
        )
        ShareSpaceSheet(
            shareSpaceLinkUIParams = shareSpaceLinkUIParams,
            enableInviteUser = true,
            isInvitingUser = false,
            addUserToSpace = { _, _ -> },
            onFinishInvitation = {}
        )
    }
}
