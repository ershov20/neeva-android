// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.cardgrid.archived

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.neeva.app.LocalSharedPreferencesModel
import com.neeva.app.R
import com.neeva.app.browsing.ArchiveAfterOption
import com.neeva.app.sharedprefs.SharedPrefFolder.App.AutomaticallyArchiveTabs
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.RadioButtonGroup
import com.neeva.app.ui.widgets.RadioButtonItem

@Composable
fun ArchivingOptionsDialog(onDismissDialog: () -> Unit) {
    val sharedPreferencesModel = LocalSharedPreferencesModel.current
    val currentValue = AutomaticallyArchiveTabs
        .getFlow(LocalSharedPreferencesModel.current)
        .collectAsState()
    val currentIndex = ArchiveAfterOption.values().indexOf(currentValue.value)

    AlertDialog(
        onDismissRequest = onDismissDialog,
        confirmButton = {
            TextButton(onClick = onDismissDialog) {
                Text(stringResource(android.R.string.ok))
            }
        },
        title = {
            Text(stringResource(R.string.archived_tabs_archive))
        },
        text = {
            Column {
                Text(stringResource(id = R.string.archived_tabs_description))

                Spacer(Modifier.height(Dimensions.PADDING_LARGE))

                RadioButtonGroup(
                    radioOptions = ArchiveAfterOption
                        .values()
                        .map { RadioButtonItem(title = it.resourceId) },
                    selectedOptionIndex = currentIndex,
                    onSelect = {
                        AutomaticallyArchiveTabs.set(
                            sharedPreferencesModel,
                            ArchiveAfterOption.values()[it]
                        )
                    }
                )
            }
        }
    )
}
