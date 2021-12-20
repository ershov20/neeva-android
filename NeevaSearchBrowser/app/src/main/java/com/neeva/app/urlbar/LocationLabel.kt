package com.neeva.app.urlbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.widgets.Button

@Composable
fun LocationLabel(
    urlBarValue: String,
    showLock: Boolean,
    onReload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(MaterialTheme.colors.primaryVariant)
            .wrapContentSize(Alignment.Center)
            .defaultMinSize(minHeight = 40.dp)
            .fillMaxWidth()
    ) {
        Spacer(modifier = Modifier.weight(1.0f))

        if (showLock) {
            Image(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_lock_18),
                contentDescription = "query icon",
                modifier = Modifier
                    .padding(8.dp)
                    .size(14.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onPrimary),
                contentScale = ContentScale.Fit
            )
        }

        Text(
            text = urlBarValue.ifEmpty { "Search or enter address" },
            style = MaterialTheme.typography.body1,
            maxLines = 1,
            color = if (urlBarValue.isEmpty()) {
                MaterialTheme.colors.onSecondary
            } else {
                MaterialTheme.colors.onPrimary
            }
        )

        Spacer(modifier = Modifier.weight(1.0f))

        Button(enabled = true,
            resID = R.drawable.ic_baseline_refresh_24,
            contentDescription = "refresh button",
            onClick = onReload
        )
    }
}

@Preview("No autocomplete, with lock, 1x font scale")
@Preview("No autocomplete, with lock, 2x font scale", fontScale = 2.0f)
@Composable
fun LocationBar_PreviewWithLock() {
    NeevaTheme {
        LocationLabel(
            urlBarValue = "https://reddit.com",
            showLock = true,
            onReload = {}
        )
    }
}

@Preview("No autocomplete, no lock, 1x font scale")
@Preview("No autocomplete, no lock, 2x font scale", fontScale = 2.0f)
@Composable
fun LocationBar_PreviewNoLock() {
    NeevaTheme {
        LocationLabel(
            urlBarValue = "https://reddit.com",
            showLock = false,
            onReload = {}
        )
    }
}

@Preview("Autocomplete, no lock, 1x font scale")
@Preview("Autocomplete, no lock, 2x font scale", fontScale = 2.0f)
@Composable
fun LocationBar_PreviewNoLockWithAutocomplete() {
    NeevaTheme {
        LocationLabel(
            urlBarValue = "https://reddit.com",
            showLock = false,
            onReload = {}
        )
    }
}