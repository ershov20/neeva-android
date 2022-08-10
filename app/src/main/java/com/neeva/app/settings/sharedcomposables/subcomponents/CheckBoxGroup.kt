package com.neeva.app.settings.sharedcomposables.subcomponents

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.neeva.app.R
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.widgets.StackedText

data class CheckBoxItem(
    @StringRes val title: Int,
    @StringRes val description: Int?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckBoxGroup(
    checkBoxOptions: List<CheckBoxItem>?,
    selectedOptionsIndex: Set<Int>?,
    onCheckedChange: (Int, Boolean) -> Unit
) {
    if (checkBoxOptions?.isEmpty() != false) {
        return
    }
    Column {
        checkBoxOptions.forEachIndexed { index, _ ->
            val isChecked = selectedOptionsIndex?.contains(index) ?: false
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .selectable(
                        selected = isChecked,
                        onClick = { onCheckedChange(index, !isChecked) }
                    )
                    .padding(vertical = Dimensions.PADDING_SMALL)
                    .fillMaxWidth()
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = { onCheckedChange(index, !isChecked) }
                )
                checkBoxOptions[index].apply {
                    StackedText(
                        primaryLabel = stringResource(title),
                        secondaryLabel = description?.let { stringResource(it) },
                        primaryMaxLines = 2,
                        secondaryMaxLines = 2,
                        modifier = Modifier.padding(horizontal = Dimensions.PADDING_TINY)
                    )
                }
            }
        }
    }
}

@PortraitPreviews
@Composable
fun CheckBoxGroup_Preview() {
    LightDarkPreviewContainer {
        CheckBoxGroup(
            checkBoxOptions = listOf(
                CheckBoxItem(
                    R.string.cookie_marketing_title,
                    R.string.cookie_marketing_description
                ),
                CheckBoxItem(
                    R.string.cookie_analytics_title,
                    R.string.cookie_analytics_description
                ),
                CheckBoxItem(
                    R.string.cookie_social_title,
                    R.string.cookie_social_description
                ),
                CheckBoxItem(
                    R.string.debug_long_string_primary,
                    R.string.debug_long_string_secondary
                )

            ),
            selectedOptionsIndex = setOf(0, 2),
            onCheckedChange = { _, _ -> }
        )
    }
}
