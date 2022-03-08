package com.neeva.app.settings.sharedComposables

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neeva.app.ui.theme.Dimensions

object SettingsUIConstants {
    val rowModifier =
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = Dimensions.PADDING_LARGE)
}
