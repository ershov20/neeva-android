package com.neeva.app.suggestions

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.storage.entities.Favicon.Companion.toBitmap
import com.neeva.app.storage.favicons.FaviconCache
import com.neeva.app.ui.LightDarkPreviewContainer

@Composable
fun NavSuggestion(
    faviconCache: FaviconCache,
    onOpenUrl: (Uri) -> Unit,
    onLogSuggestionTap: ((SuggestionType, Int?) -> Unit)? = null,
    navSuggestion: NavSuggestion
) {
    val faviconBitmap: Bitmap? by faviconCache.getFaviconAsync(navSuggestion.url)
    NavSuggestion(
        faviconBitmap = faviconBitmap,
        onOpenUrl = onOpenUrl,
        onLogSuggestionTap = onLogSuggestionTap,
        navSuggestion = navSuggestion
    )
}

@Composable
fun NavSuggestion(
    faviconBitmap: Bitmap?,
    onOpenUrl: (Uri) -> Unit,
    onLogSuggestionTap: ((SuggestionType, Int?) -> Unit)? = null,
    navSuggestion: NavSuggestion
) {
    NavSuggestionRow(
        iconParams = SuggestionRowIconParams(
            faviconBitmap = faviconBitmap
        ),
        primaryLabel = navSuggestion.label,
        onTapRow = {
            onOpenUrl(navSuggestion.url)
            onLogSuggestionTap?.invoke(navSuggestion.type, navSuggestion.position)
        },
        secondaryLabel = navSuggestion.secondaryLabel
    )
}

@Preview(name = "LTR, 1x font size", locale = "en")
@Preview(name = "LTR, 2x font size", locale = "en", fontScale = 2.0f)
@Preview(name = "RTL, 1x font size", locale = "he")
@Composable
fun NavSuggestion_Preview() {
    LightDarkPreviewContainer {
        NavSuggestion(
            faviconBitmap = null,
            onOpenUrl = {},
            navSuggestion = NavSuggestion(
                url = Uri.parse("https://www.neeva.com"),
                label = stringResource(id = R.string.debug_long_string_primary),
                secondaryLabel = stringResource(id = R.string.debug_long_string_secondary),
                type = SuggestionType.MEMORIZED_SUGGESTION
            )
        )
    }
}

@Preview(name = "Solid favicon, 1x font size", locale = "en")
@Preview(name = "Solid favicon, 2x font size", locale = "en", fontScale = 2.0f)
@Composable
fun NavSuggestion_PreviewWithSolidFavicon() {
    val uri = Uri.parse("https://www.neeva.com")
    LightDarkPreviewContainer {
        NavSuggestion(
            faviconBitmap = uri.toBitmap(),
            onOpenUrl = {},
            navSuggestion = NavSuggestion(
                url = uri,
                label = "Primary label",
                secondaryLabel = "Secondary label",
                type = SuggestionType.MEMORIZED_SUGGESTION
            )
        )
    }
}
