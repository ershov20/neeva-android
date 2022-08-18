package com.neeva.app.cardgrid.tabs

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.res.stringResource
import com.neeva.app.LocalSharedPreferencesModel
import com.neeva.app.R
import com.neeva.app.browsing.ArchiveAfterOption
import com.neeva.app.sharedprefs.SharedPrefFolder.App.AutomaticallyArchiveTabs
import com.neeva.app.ui.widgets.RadioButtonGroup

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
            RadioButtonGroup(
                radioOptions = ArchiveAfterOption.values().map { stringResource(it.resourceId) },
                selectedOptionIndex = currentIndex,
                onSelect = {
                    AutomaticallyArchiveTabs.set(
                        sharedPreferencesModel,
                        ArchiveAfterOption.values()[it]
                    )
                }
            )
        }
    )
}
