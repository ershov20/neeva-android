// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.package com.neeva.app.spaces.share
package com.neeva.app.spaces.share

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.neeva.app.R
import com.neeva.app.firstrun.widgets.textfields.ClearFocusOnDismissTextField
import com.neeva.app.type.SpaceACLLevel
import com.neeva.app.type.SpaceEmailACL
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.getClickableAlpha
import com.neeva.app.ui.widgets.collapsingsection.CollapsingSectionState
import com.neeva.app.ui.widgets.collapsingsection.collapsingSection
import com.neeva.app.ui.widgets.collapsingsection.setNextState

@Composable
fun InviteUserUI(
    isInvitingUser: Boolean,
    addUserToSpace: (shareWith: SpaceEmailACL, note: String?) -> Unit,
    onFinishInvitation: () -> Unit
) {
    val isInvitingUserState = remember {
        if (isInvitingUser) {
            mutableStateOf(CollapsingSectionState.EXPANDED)
        } else {
            mutableStateOf(CollapsingSectionState.COLLAPSED)
        }
    }

    val email = rememberSaveable { mutableStateOf("") }
    val note = rememberSaveable { mutableStateOf("") }
    val selectedACLLevel = rememberSaveable { mutableStateOf(SpaceACLLevel.View) }

    Column {
        LazyColumn(Modifier.weight(1f)) {
            collapsingSection(
                label = R.string.space_invite_someone,
                collapsingSectionState = isInvitingUserState.value,
                onUpdateCollapsingSectionState = isInvitingUserState::setNextState
            ) {
                item {
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .padding(Dimensions.PADDING_MEDIUM)
                            .fillMaxHeight()
                    ) {
                        ClearFocusOnDismissTextField(
                            text = email.value,
                            onTextChanged = { email.value = it },
                            label = stringResource(id = R.string.email_label)
                        )

                        Spacer(Modifier.height(Dimensions.PADDING_MEDIUM))

                        ACLDropdownPicker(selectedACLLevel)

                        // For some reason the ExposedDropdownMenuBox has an additional 8.dp of
                        // hidden padding below it. Not sure why, it could be a bug as it is
                        // Experimental.
                        Spacer(Modifier.height(Dimensions.PADDING_TINY))

                        ClearFocusOnDismissTextField(
                            text = note.value,
                            onTextChanged = { note.value = it },
                            label = stringResource(id = R.string.space_note_label)
                        )

                        Spacer(Modifier.height(Dimensions.PADDING_MEDIUM))

                        InviteButton(enabled = isValidEmail(email.value)) {
                            addUserToSpace(
                                SpaceEmailACL(email.value, selectedACLLevel.value),
                                note.value
                            )
                            onFinishInvitation()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.alpha(getClickableAlpha(enabled))
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_invite),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Spacer(modifier = Modifier.size(Dimensions.PADDING_SMALL))

            Text(
                text = stringResource(id = R.string.space_invite_label),
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ACLDropdownPicker(selectedACLLevel: MutableState<SpaceACLLevel>) {
    val expanded = remember { mutableStateOf(false) }
    val aclLevels = listOf(SpaceACLLevel.View, SpaceACLLevel.Comment, SpaceACLLevel.Edit)

    ExposedDropdownMenuBox(
        expanded = expanded.value,
        onExpandedChange = {
            expanded.value = !expanded.value
        }
    ) {
        OutlinedTextField(
            value = getACLText(selectedACLLevel.value),
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded.value)
            },
            modifier = Modifier.fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            aclLevels.forEach { aclLevel ->
                DropdownMenuItem(
                    text = { Text(text = getACLText(aclLevel)) },
                    onClick = {
                        selectedACLLevel.value = aclLevel
                        expanded.value = false
                    }
                )
            }
        }
    }
}

@Composable
private fun getACLText(aclLevel: SpaceACLLevel): String {
    return when (aclLevel) {
        SpaceACLLevel.View -> stringResource(id = R.string.share_space_can_view_permission)
        SpaceACLLevel.Comment -> stringResource(id = R.string.share_space_can_comment_permission)
        SpaceACLLevel.Edit -> stringResource(id = R.string.share_space_can_edit_permission)
        else -> ""
    }
}

private fun isValidEmail(email: String): Boolean {
    return email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
}
