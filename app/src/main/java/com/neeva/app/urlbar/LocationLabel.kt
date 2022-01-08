package com.neeva.app.urlbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
    ) {
        Spacer(modifier = Modifier.weight(1.0f))

        if (showLock) {
            Image(
                painter = painterResource(R.drawable.ic_baseline_lock_18),
                contentDescription = "secure site",
                modifier = Modifier
                    .padding(8.dp)
                    .size(14.dp),
                colorFilter = ColorFilter.tint(
                    MaterialTheme.colors.contentColorFor(MaterialTheme.colors.primaryVariant)
                ),
                contentScale = ContentScale.Fit
            )
        }

        Text(
            text = urlBarValue.ifEmpty { "Search or enter address" },
            style = MaterialTheme.typography.body1,
            maxLines = 1,
            color = MaterialTheme.colors.contentColorFor(MaterialTheme.colors.primaryVariant)
        )

        Spacer(modifier = Modifier.weight(1.0f))

        Button(
            enabled = true,
            resID = R.drawable.ic_baseline_refresh_24,
            contentDescription = stringResource(R.string.reload),
            onClick = onReload
        )
    }
}

@Preview("With lock, dark mode, 1x font scale")
@Preview("With lock, dark mode, 2x font scale", fontScale = 2.0f)
@Composable
fun LocationBar_PreviewWithLock_Dark() {
    NeevaTheme(darkTheme = true) {
        LocationLabel(
            urlBarValue = "www.reddit.com",
            showLock = true,
            onReload = {}
        )
    }
}

@Preview("With lock, 1x font scale")
@Preview("With lock, 2x font scale", fontScale = 2.0f)
@Composable
fun LocationBar_PreviewWithLock() {
    NeevaTheme {
        LocationLabel(
            urlBarValue = "www.reddit.com",
            showLock = true,
            onReload = {}
        )
    }
}

@Preview("No lock, 1x font scale")
@Preview("No lock, 2x font scale", fontScale = 2.0f)
@Composable
fun LocationBar_PreviewNoLock() {
    NeevaTheme {
        LocationLabel(
            urlBarValue = "www.reddit.com",
            showLock = false,
            onReload = {}
        )
    }
}
