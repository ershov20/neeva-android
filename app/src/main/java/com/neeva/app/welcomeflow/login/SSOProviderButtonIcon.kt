// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.welcomeflow.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.neeva.app.R
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.userdata.NeevaUser

@Composable
fun SSOProviderButtonIcon(ssoProvider: NeevaUser.SSOProvider) {
    when (ssoProvider) {
        NeevaUser.SSOProvider.GOOGLE, NeevaUser.SSOProvider.OKTA -> {
            Icon(
                painter = SSOImagePainter(ssoProvider),
                contentDescription = null,
                modifier = Modifier.size(Dimensions.SIZE_ICON_MEDIUM)
            )
        }
        NeevaUser.SSOProvider.MICROSOFT -> {
            Image(
                painter = SSOImagePainter(ssoProvider),
                contentDescription = null,
                modifier = Modifier.size(Dimensions.SIZE_ICON_MEDIUM)
            )
        }
        else -> { }
    }
}

@Composable
fun SSOImagePainter(ssoProvider: NeevaUser.SSOProvider): Painter {
    return when (ssoProvider) {
        NeevaUser.SSOProvider.GOOGLE -> painterResource(id = R.drawable.ic_google)
        NeevaUser.SSOProvider.MICROSOFT -> painterResource(id = R.drawable.ic_microsoft)
        else -> painterResource(id = R.drawable.ic_email)
    }
}
