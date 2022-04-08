package com.neeva.app.firstrun.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.neeva.app.R
import com.neeva.app.firstrun.FirstRunConstants
import com.neeva.app.ui.theme.Dimensions

@Composable
fun OrSeparator() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Divider(
            modifier = Modifier
                .weight(1f)
                .background(color = MaterialTheme.colorScheme.outline)
        )

        Text(
            text = stringResource(R.string.or).uppercase(),
            style = FirstRunConstants.getSubtextStyle(MaterialTheme.colorScheme.outline),
            modifier = Modifier.padding(horizontal = Dimensions.PADDING_LARGE)
        )

        Divider(
            modifier = Modifier
                .weight(1f)
                .background(color = MaterialTheme.colorScheme.outline)
        )
    }
}
