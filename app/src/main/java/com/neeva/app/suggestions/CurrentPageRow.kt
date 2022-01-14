package com.neeva.app.suggestions

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun CurrentPageRow(
    faviconBitmap: Bitmap?,
    url: Uri,
    onEditPressed: () -> Unit
) {
    NavSuggestionRow(
        primaryLabel = url.toString(),
        onTapRow = { onEditPressed.invoke() },
        secondaryLabel = stringResource(id = R.string.edit_current_url),
        onTapEdit = onEditPressed,
        faviconBitmap = faviconBitmap
    )
}

@Preview("Light, 1x font scale", locale = "en")
@Preview("Light, 2x font scale", locale = "en", fontScale = 2.0f)
@Preview("Light, RTL, 1x font scale", locale = "he")
@Preview("Light, RTL, 2x font scale", locale = "he", fontScale = 2.0f)
@Composable
fun CurrentPageRow_PreviewLight() {
    NeevaTheme(useDarkTheme = false) {
        CurrentPageRow(
            faviconBitmap = null,
            url = Uri.parse("https://www.reddit.com")
        ) {}
    }
}

@Preview("Dark, 1x font scale", locale = "en")
@Preview("Dark, 2x font scale", locale = "en", fontScale = 2.0f)
@Composable
fun CurrentPageRow_PreviewDark() {
    NeevaTheme(useDarkTheme = true) {
        CurrentPageRow(
            faviconBitmap = null,
            url = Uri.parse("https://www.reddit.com")
        ) {}
    }
}
