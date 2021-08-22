package com.neeva.app.settings

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.neeva.app.AppNavModel
import com.neeva.app.R

@ExperimentalFoundationApi
@Composable
fun SettingsMain(appNavModel: AppNavModel) {
    LazyColumn(
        Modifier.fillMaxSize()
    ) {
        stickyHeader {
            Row(modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colors.primary),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_arrow_back_24),
                    contentDescription = "Close Settings",
                    contentScale = ContentScale.Inside,
                    modifier = Modifier
                        .size(48.dp, 48.dp)
                        .clickable { appNavModel.setVisibility(false)},
                    colorFilter = ColorFilter.tint(MaterialTheme.colors.onPrimary)
                )
                Text(
                    text = "Settings",
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.h5,
                    color = MaterialTheme.colors.onPrimary,
                    maxLines = 1,
                )
            }
        }

        SettingsMainData.groups.forEach {
            item {
                Box(modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp)
                    .background(MaterialTheme.colors.background)
                    .wrapContentHeight(align = Alignment.Bottom),
                ) {
                    Text(
                        text = it.label,
                        style = MaterialTheme.typography.h6,
                        color = MaterialTheme.colors.onPrimary,
                        maxLines = 1,
                    )
                }
            }

            items(it.rows) { row ->
                SettingsRow(data = row, appNavModel)
            }
        }
    }
}

@Composable
fun SettingsRow(data: SettingsRowData, appNavModel: AppNavModel) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
        .background(MaterialTheme.colors.primary)
        .then(if (data.type == SettingsRowType.LINK) Modifier.clickable {
            appNavModel.onOpenUrl(data.url!!)
            appNavModel.setVisibility(false)
        } else Modifier),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = data.title,
            style = MaterialTheme.typography.subtitle1,
            color = MaterialTheme.colors.onPrimary,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.weight(1.0f))
        if (data.type == SettingsRowType.LINK) {
            Image(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_open_in_new_24),
                contentDescription = data.title,
                contentScale = ContentScale.Inside,
                modifier = Modifier
                    .size(48.dp, 48.dp),
                colorFilter = ColorFilter.tint(MaterialTheme.colors.onSecondary)
            )
        }
    }
}