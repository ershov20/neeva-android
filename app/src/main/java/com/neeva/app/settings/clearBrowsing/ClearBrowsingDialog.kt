package com.neeva.app.settings.clearBrowsing

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.neeva.app.LocalSettingsDataModel
import com.neeva.app.R
import com.neeva.app.settings.sharedComposables.SettingsDialog

@Composable
fun ClearBrowsingDialog(
    confirmAction: (TimeClearingOption) -> Unit,
    dismissAction: () -> Unit,
) {
    val settingsDataModel = LocalSettingsDataModel.current
    val selectedOptionIndex = settingsDataModel.getTimeClearingOptionIndex()
    val radioOptions = TimeClearingOption.values().map { stringResource(it.string_id) }
    SettingsDialog(
        textId = R.string.clear_browsing_dialog_text,
        radioOptions = radioOptions,
        selectedOptionIndex = selectedOptionIndex,
        saveSelectedOptionIndex = settingsDataModel::saveSelectedTimeClearingOption,
        confirmStringId = R.string.clear_browsing_clear_data,
        confirmAction = { selectedIndex ->
            if (selectedIndex != null) {
                val timeClearingOption = TimeClearingOption.values().getOrNull(selectedIndex)
                // instead of silently failing, you will get an error if that TimeClearingOption is not found.
                confirmAction(timeClearingOption!!)
            }
        },
        dismissStringId = R.string.cancel,
        dismissAction = dismissAction
    )
}
