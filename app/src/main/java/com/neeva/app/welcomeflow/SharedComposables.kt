// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.welcomeflow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.theme.Dimensions

@Composable
internal fun WelcomeFlowButton(
    primaryText: String,
    secondaryText: String? = null,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = Dimensions.PADDING_SMALL)
        ) {
            Text(
                text = primaryText,
                style = MaterialTheme.typography.titleMedium
            )
            if (secondaryText != null) {
                Text(
                    text = secondaryText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun BenefitTitle(title: String, style: TextStyle) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_check_24),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(Dimensions.PADDING_LARGE))
        Text(
            text = title,
            style = style,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private val benefitSecondaryTextPadding = 48.dp

@Composable
internal fun MainBenefit(title: String, description: String? = null) {
    Column {
        BenefitTitle(title = title, style = MaterialTheme.typography.titleLarge)
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = benefitSecondaryTextPadding)
            )
        }
    }
}

@Composable
internal fun PlansBenefit(title: String, description: String? = null) {
    Column {
        BenefitTitle(title = title, style = MaterialTheme.typography.titleMedium)
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = benefitSecondaryTextPadding)
            )
        }
    }
}
