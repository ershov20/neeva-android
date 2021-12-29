package com.neeva.app.suggestions

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.browsing.toFavicon
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

@Preview(name = "1x font size", locale = "en")
@Preview(name = "2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun NavSuggestion_Preview() {
    NeevaTheme {
        NavSuggestion(
            faviconData = null,
            onOpenUrl = {},
            navSuggestion = NavSuggestion(
                url = Uri.parse("https://www.neeva.com"),
                label = stringResource(id = R.string.debug_long_string_english_primary),
                secondaryLabel = stringResource(id = R.string.debug_long_string_english_secondary)
            )
        )
    }
}

@Preview(name = "RTL, 1x font size", locale = "he")
@Preview(name = "RTL, 2x font size", locale = "he", fontScale = 2.0f)
@Composable
fun NavSuggestion_PreviewHebrew() {
    NeevaTheme {
        NavSuggestion(
            faviconData = null,
            onOpenUrl = {},
            navSuggestion = NavSuggestion(
                url = Uri.parse("https://www.neeva.com"),
                label = stringResource(id = R.string.debug_long_string_hebrew),
                secondaryLabel = stringResource(id = R.string.debug_long_string_hebrew)
            )
        )
    }
}

@Preview(name = "Solid favicon, 1x font size", locale = "en")
@Preview(name = "Solid favicon, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun NavSuggestion_PreviewWithSolidFavicon() {
    val uri = Uri.parse("https://www.neeva.com")
    NeevaTheme {
        NavSuggestion(
            faviconData = uri.toFavicon().toBitmap(),
            onOpenUrl = {},
            navSuggestion = NavSuggestion(
                url = uri,
                label = "Primary label",
                secondaryLabel = "Secondary label"
            )
        )
    }
}
