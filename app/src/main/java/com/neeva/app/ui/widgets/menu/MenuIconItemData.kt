package com.neeva.app.ui.widgets.menu

import androidx.annotation.StringRes
import com.neeva.app.ui.widgets.RowActionIconParams

data class MenuIconItemData(
    val id: Int,
    @StringRes val labelId: Int,
    val action: RowActionIconParams.ActionType,
    val enabled: Boolean = true
)
