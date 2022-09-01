// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui.widgets.icons

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.neeva.app.R
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.userdata.NeevaUser

@Composable
fun SSOProviderImage(ssoProvider: NeevaUser.SSOProvider) {
    Image(
        painter = SSOImagePainter(ssoProvider) ?: painterResource(R.drawable.ic_default_avatar),
        contentDescription = null,
        modifier = Modifier.size(Dimensions.SIZE_ICON_MEDIUM)
    )
}

@Composable
fun SSOImagePainter(ssoProvider: NeevaUser.SSOProvider): Painter? {
    val painter = when (ssoProvider) {
        NeevaUser.SSOProvider.GOOGLE -> painterResource(id = R.drawable.ic_google)
        NeevaUser.SSOProvider.MICROSOFT -> painterResource(id = R.drawable.ic_microsoft)
        NeevaUser.SSOProvider.OKTA -> painterResource(id = R.drawable.ic_neeva_logo)
        else -> null
    }
    return painter
}

fun getFormattedSSOProviderName(ssoProvider: NeevaUser.SSOProvider): String {
    return when (ssoProvider) {
        NeevaUser.SSOProvider.GOOGLE -> "Google"
        NeevaUser.SSOProvider.MICROSOFT -> "Microsoft"
        NeevaUser.SSOProvider.OKTA -> "Okta"
        NeevaUser.SSOProvider.APPLE -> "Apple"
        NeevaUser.SSOProvider.UNKNOWN -> "Unknown"
    }
}
