// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.zeroquery

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.theme.Dimensions

@Composable
fun AdBlockerPromo() {
    Surface(
        shape = RoundedCornerShape(Dimensions.RADIUS_SMALL),
        shadowElevation = 2.dp,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.padding(Dimensions.PADDING_LARGE)
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimensions.PADDING_HUGE)
        ) {
            val padding = 10.dp

            Icon(
                painter = painterResource(R.drawable.ic_shield),
                contentDescription = stringResource(
                    R.string.content_filter_content_description
                ),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .size(50.dp)
                    .offset(x = -padding)
            )

            Spacer(modifier = Modifier.height(Dimensions.PADDING_LARGE))

            Text(
                text = stringResource(id = R.string.first_run_ad_block_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(Dimensions.PADDING_LARGE))

            Text(
                text = stringResource(id = R.string.first_run_ad_block_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
