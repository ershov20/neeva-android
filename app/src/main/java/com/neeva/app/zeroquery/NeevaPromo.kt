// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.zeroquery

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.neeva.app.R
import com.neeva.app.ui.theme.Dimensions

@Composable
fun NeevaPromo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.height(Dimensions.PADDING_LARGE))

        Image(
            painter = painterResource(id = R.drawable.neeva_letter_logo),
            contentDescription = null
        )

        Spacer(modifier = Modifier.height(Dimensions.PADDING_SMALL))

        Text(
            text = stringResource(id = R.string.first_run_ad_free),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
