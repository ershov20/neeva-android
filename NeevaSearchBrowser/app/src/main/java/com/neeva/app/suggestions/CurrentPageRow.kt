package com.neeva.app.suggestions

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.neeva.app.R
import com.neeva.app.storage.DomainViewModel
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.widgets.FaviconView

@Composable
fun CurrentPageRow(domainViewModel: DomainViewModel, url: Uri, onEditPressed: () -> Unit) {
    val favicon: Bitmap? by domainViewModel.getFaviconFor(url).observeAsState()
    CurrentPageRow(favicon = favicon, url = url, onEditPressed = onEditPressed)
}

@Composable
fun CurrentPageRow(favicon: Bitmap?, url: Uri, onEditPressed: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .padding(start = 12.dp)
    ) {
        FaviconView(favicon)
        Column(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .weight(1.0f)
        ) {
            Text(
                text = url.toString(),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onPrimary,
                maxLines = 1,
            )
            Text(
                text = "Edit current url",
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSecondary,
                maxLines = 1,
            )
        }
        Image(
            imageVector = ImageVector.vectorResource(id = R.drawable.ic_baseline_edit_24),
            contentDescription = "query icon",
            contentScale = ContentScale.Inside,
            modifier = Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .requiredSize(48.dp, 48.dp)
                .clickable { onEditPressed() },
            colorFilter = ColorFilter.tint(MaterialTheme.colors.onSecondary)
        )
    }
}

@Preview
@Preview(fontScale = 2.0f)
@Composable
fun CurrentPageRow_PreviewLight() {
    NeevaTheme(darkTheme = false) {
        CurrentPageRow(
            favicon = null,
            url = Uri.parse("https://www.reddit.com")
        ) {}
    }
}

@Preview
@Preview(fontScale = 2.0f)
@Composable
fun CurrentPageRow_PreviewDark() {
    NeevaTheme(darkTheme = true) {
        CurrentPageRow(
            favicon = null,
            url = Uri.parse("https://www.reddit.com")
        ) {}
    }
}