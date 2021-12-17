package com.neeva.app.suggestions

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.storage.DomainViewModel
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun CurrentPageRow(domainViewModel: DomainViewModel, url: Uri, onEditPressed: () -> Unit) {
    val favicon: Bitmap? by domainViewModel.getFaviconFor(url).observeAsState()
    CurrentPageRow(favicon = favicon, url = url, onEditPressed = onEditPressed)
}

@Composable
fun CurrentPageRow(
    favicon: Bitmap?,
    url: Uri,
    onEditPressed: () -> Unit
) {
    SuggestionRow(
        primaryLabel = url.toString(),
        onTapRow = { onEditPressed.invoke() },
        secondaryLabel = stringResource(id = R.string.edit_current_url),
        onTapEdit = onEditPressed,
        faviconData = favicon
    )
}

@Preview("Light, 1x font scale")
@Preview("Light, 2x font scale", fontScale = 2.0f)
@Composable
fun CurrentPageRow_PreviewLight() {
    NeevaTheme(darkTheme = false) {
        CurrentPageRow(
            favicon = null,
            url = Uri.parse("https://www.reddit.com")
        ) {}
    }
}

@Preview("Dark, 1x font scale")
@Preview("Dark, 2x font scale", fontScale = 2.0f)
@Composable
fun CurrentPageRow_PreviewDark() {
    NeevaTheme(darkTheme = true) {
        CurrentPageRow(
            favicon = null,
            url = Uri.parse("https://www.reddit.com")
        ) {}
    }
}