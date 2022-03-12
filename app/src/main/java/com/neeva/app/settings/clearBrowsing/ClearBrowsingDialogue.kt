package com.neeva.app.settings.clearBrowsing

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import com.neeva.app.R
import com.neeva.app.settings.sharedComposables.SettingsDialog

@Composable
fun ClearBrowsingDialog(
    confirmAction: (TimeClearingOption) -> Unit,
    dismissAction: () -> Unit,
) {
    val radioOptions = TimeClearingOption.values().map { it.string_id }
    SettingsDialog(
        textId = R.string.clear_browsing_dialog_text,
        radioOptions = radioOptions,
        confirmStringId = R.string.clear_browsing_clear_data,
        confirmAction = { selectedOptionId ->
            val timeClearingOption = getCorrespondingTimeClearingOption(selectedOptionId)
            // Because I can't ensure that selectedOptionId has corresponding TimeClearingOption,
            // the dialog will never close if radioOptions is built from TimeClearingOption.values()
            if (timeClearingOption != null) {
                confirmAction(timeClearingOption)
            }
        },
        dismissStringId = R.string.cancel,
        dismissAction = dismissAction
    )
}

private fun getCorrespondingTimeClearingOption(@StringRes stringId: Int?): TimeClearingOption? {
    TimeClearingOption.values().forEach {
        if (it.string_id == stringId) {
            return it
        }
    }
    return null
}
