// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.widgets.menu

import androidx.annotation.StringRes
import com.neeva.app.ui.widgets.RowActionIconParams

data class MenuIconItemData(
    val id: Int,
    @StringRes val labelId: Int,
    val action: RowActionIconParams.ActionType,
    val enabled: Boolean = true
)
