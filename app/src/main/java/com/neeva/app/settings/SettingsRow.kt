package com.neeva.app.settings

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun SettingsRow(data: SettingsRowData, openUrl: (Uri) -> Unit) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .defaultMinSize(minHeight = 56.dp)
        .background(MaterialTheme.colors.primary)
        .then(
            if (data.type == SettingsRowType.LINK && data.url != null) {
                Modifier.clickable { openUrl(data.url) }
            } else {
                Modifier
            }
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = data.title,
            style = MaterialTheme.typography.h3,
            color = MaterialTheme.colors.onPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1.0f)
        )

        if (data.type == SettingsRowType.LINK) {
            Image(
                painter = painterResource(R.drawable.ic_baseline_open_in_new_24),
                contentDescription = data.title,
                contentScale = ContentScale.Inside,
                modifier = Modifier.size(48.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onSecondary)
            )
        }
    }
}

@Preview(name = "Link, 1x font size", locale = "en")
@Preview(name = "Link, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Link, RTL, 1x font size", locale = "he")
@Preview(name = "Link, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsRow_PreviewLink() {
    NeevaTheme {
        SettingsRow(
            data = SettingsRowData(
                stringResource(R.string.debug_long_string_primary),
                SettingsRowType.LINK,
                Uri.parse("")
            ),
            openUrl = {}
        )
    }
}

@Preview(name = "Label, 1x font size", locale = "en")
@Preview(name = "Label, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun SettingsRow_PreviewLabel() {
    NeevaTheme {
        SettingsRow(
            data = SettingsRowData(
                stringResource(R.string.debug_long_string_primary),
                SettingsRowType.LABEL
            ),
            openUrl = {}
        )
    }
}
