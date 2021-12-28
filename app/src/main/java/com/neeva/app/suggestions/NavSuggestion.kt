package com.neeva.app.suggestions

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.browsing.toFaviconBitmap
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun NavSuggestion(
    faviconData: Bitmap?,
    onOpenUrl: (Uri) -> Unit,
    navSuggestion: NavSuggestion
) {
    SuggestionRow(
        primaryLabel = navSuggestion.label,
        onTapRow = { onOpenUrl.invoke(navSuggestion.url) },
        secondaryLabel = navSuggestion.secondaryLabel,
        faviconData = faviconData
    )
}

@Preview(name = "1x font size")
@Preview(name = "2x font size", fontScale = 2.0f)
@Composable
fun NavSuggestion_Preview() {
    NeevaTheme {
        NavSuggestion(
            faviconData = null,
            onOpenUrl = {},
            navSuggestion = NavSuggestion(
                url = Uri.parse("https://www.neeva.com"),
                label = "Primary label",
                secondaryLabel = "Secondary label"
            )
        )
    }
}

@Preview(name = "Solid favicon, 1x font size")
@Preview(name = "Solid favicon, 2x font size", fontScale = 2.0f)
@Composable
fun NavSuggestion_PreviewWithSolidFavicon() {
    val uri = Uri.parse("https://www.neeva.com")
    NeevaTheme {
        NavSuggestion(
            faviconData = uri.toFaviconBitmap(),
            onOpenUrl = {},
            navSuggestion = NavSuggestion(
                url = uri,
                label = "Primary label",
                secondaryLabel = "Secondary label"
            )
        )
    }
}