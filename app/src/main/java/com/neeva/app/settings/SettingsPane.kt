package com.neeva.app.settings

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.IconButton
import androidx.compose.material.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.storage.NeevaUser
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun SettingsPane(
    onShowBrowser: () -> Unit,
    onOpenUrl: (Uri) -> Unit,
    onClearHistory: () -> Unit,
    getTogglePreferenceSetter: (String?) -> ((Boolean) -> Unit)?,
    getToggleState: (String?) -> MutableState<Boolean>?
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize(),
    ) {
        TopAppBar(
            title = {
                Text(
                    text = stringResource(R.string.settings),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            backgroundColor = MaterialTheme.colorScheme.surface,
            navigationIcon = {
                IconButton(
                    onClick = { onShowBrowser() }
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_baseline_arrow_back_24),
                        contentDescription = stringResource(R.string.close),
                        contentScale = ContentScale.Inside,
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                    )
                }
            }
        )

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
                            .background(MaterialTheme.colorScheme.surface)
                            .wrapContentHeight(align = Alignment.Bottom),
                    ) {
                        // TODO(kobec): might be wrong font style
                        Text(
                            text = stringResource(it.title_id),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                    }
                }

                items(it.rows) { rowData ->
                    if (rowData.type == SettingsRowType.PROFILE) {
                        ProfileUI(id = NeevaUser.shared.id)
                    } else {
                        SettingsRow(
                            data = rowData,
                            openUrl = onOpenUrl,
                            onClearHistory = onClearHistory,
                            getTogglePreferenceSetter = getTogglePreferenceSetter,
                            getToggleState = getToggleState
                        )
                    }
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
        SettingsPane(
            onShowBrowser = {},
            onOpenUrl = {},
            onClearHistory = {},
            getTogglePreferenceSetter = { {} },
            getToggleState = { mutableStateOf(true) }
        )
    }
}
