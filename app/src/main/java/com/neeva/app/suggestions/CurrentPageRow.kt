package com.neeva.app.suggestions

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.storage.Favicon
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun CurrentPageRow(
    favicon: Favicon?,
    url: Uri,
    onEditPressed: () -> Unit
) {
    NavSuggestionRow(
        primaryLabel = url.toString(),
        onTapRow = { onEditPressed.invoke() },
        secondaryLabel = stringResource(id = R.string.edit_current_url),
        onTapEdit = onEditPressed,
        faviconData = favicon
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
            favicon = null,
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
            favicon = null,
            url = Uri.parse("https://www.reddit.com")
        ) {}
    }
}
