package com.neeva.app.settings

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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

@Composable
fun SettingsPane(
    onShowBrowser: () -> Unit,
    onOpenUrl: (Uri) -> Unit
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.background)
            .fillMaxSize(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .background(MaterialTheme.colors.primary),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                contentDescription = stringResource(R.string.close),
                contentScale = ContentScale.Inside,
                modifier = Modifier
                    .size(48.dp)
                    .clickable { onShowBrowser() },
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onPrimary)
            )
            Text(
                text = stringResource(R.string.settings),
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.h3,
                color = MaterialTheme.colors.onPrimary,
                maxLines = 1,
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxWidth()
        ) {
            SettingsMainData.groups.forEach {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .padding(16.dp)
                            .background(MaterialTheme.colors.background)
                            .wrapContentHeight(align = Alignment.Bottom),
                    ) {
                        Text(
                            text = it.label,
                            style = MaterialTheme.typography.h4,
                            color = MaterialTheme.colors.onPrimary,
                            maxLines = 1,
                        )
                    }
                }

                items(it.rows) { row ->
                    SettingsRow(data = row, openUrl = onOpenUrl)
                }
            }
        }
    }
}

@Preview(name = "Full settings, 1x font size", locale = "en")
@Preview(name = "Full settings, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "Full settings, RTL, 1x font size", locale = "he")
@Preview(name = "Full settings, RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun SettingsMain_Preview() {
    NeevaTheme {
        SettingsPane({}, {})
    }
}
