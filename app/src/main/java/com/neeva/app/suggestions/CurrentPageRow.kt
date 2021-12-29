package com.neeva.app.suggestions

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.history.DomainViewModel
import com.neeva.app.storage.Favicon
import com.neeva.app.ui.theme.NeevaTheme

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

@Preview("Light, 1x font scale", locale = "en")
@Preview("Light, 2x font scale", locale = "en", fontScale = 2.0f)
@Preview("Light, Hebrew, 1x font scale", locale = "he")
@Preview("Light, Hebrew, 2x font scale", locale = "he", fontScale = 2.0f)
@Composable
fun CurrentPageRow_PreviewLight() {
    NeevaTheme(darkTheme = false) {
        CurrentPageRow(
            favicon = null,
            url = Uri.parse("https://www.reddit.com")
        ) {}
    }
}

@Preview("Dark, 1x font scale", locale = "en")
@Preview("Dark, 2x font scale", locale = "en", fontScale = 2.0f)
@Composable
fun CurrentPageRow_PreviewDark() {
    NeevaTheme(darkTheme = true) {
        CurrentPageRow(
            favicon = null,
            url = Uri.parse("https://www.reddit.com")
        ) {}
    }
}