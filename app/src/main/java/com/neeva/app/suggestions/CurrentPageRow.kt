package com.neeva.app.suggestions

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.ui.theme.NeevaTheme

@Composable
fun CurrentPageRow(browserWrapper: BrowserWrapper) {
    val activeTabModel = browserWrapper.activeTabModel
    val faviconCache = browserWrapper.faviconCache
    val urlBarModel = browserWrapper.urlBarModel

    val displayedInfo by activeTabModel.displayedInfoFlow.collectAsState()
    val currentURL: Uri by activeTabModel.urlFlow.collectAsState()
    val isLazyTab: Boolean by browserWrapper.isLazyTabFlow.collectAsState()

    val displayedText = displayedInfo.displayedText
    val isShowingPlaceholder = displayedInfo.mode == ActiveTabModel.DisplayMode.PLACEHOLDER
    val isShowingQuery = displayedInfo.mode == ActiveTabModel.DisplayMode.QUERY

    val faviconBitmap by faviconCache.getFaviconAsync(currentURL)

    if (!isLazyTab && !isShowingPlaceholder && currentURL.toString().isNotBlank()) {
        val label = if (isShowingQuery) {
            displayedText
        } else {
            currentURL.toString()
        }

        CurrentPageRow(
            faviconBitmap = faviconBitmap,
            label = label,
            isShowingQuery = isShowingQuery
        ) {
            urlBarModel.replaceLocationBarText(
                if (isShowingQuery) displayedText else currentURL.toString()
            )
        }
    }
}

@Composable
fun CurrentPageRow(
    faviconBitmap: Bitmap?,
    label: String,
    isShowingQuery: Boolean,
    onEditPressed: () -> Unit
) {
    NavSuggestionRow(
        primaryLabel = label,
        onTapRow = { onEditPressed.invoke() },
        secondaryLabel = stringResource(
            id = if (isShowingQuery) {
                R.string.edit_current_search
            } else {
                R.string.edit_current_url
            }
        ),
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
            label = "https://www.reddit.com",
            isShowingQuery = false
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
            label = "https://www.reddit.com",
            isShowingQuery = false
        ) {}
    }
}
