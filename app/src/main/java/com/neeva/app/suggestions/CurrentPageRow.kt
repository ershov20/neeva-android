package com.neeva.app.suggestions

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.neeva.app.R
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.widgets.RowActionIconParams

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

        Column {
            CurrentPageRow(
                faviconBitmap = faviconBitmap,
                label = label,
                isShowingQuery = isShowingQuery
            ) {
                urlBarModel.replaceLocationBarText(
                    if (isShowingQuery) displayedText else currentURL.toString()
                )
            }

            SuggestionDivider()
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
        iconParams = SuggestionRowIconParams(
            faviconBitmap = faviconBitmap
        ),

        primaryLabel = label,
        onTapRow = { onEditPressed.invoke() },
        secondaryLabel = stringResource(
            id = if (isShowingQuery) {
                R.string.edit_current_search
            } else {
                R.string.edit_current_url
            }
        ),
        actionIconParams = RowActionIconParams(
            onTapAction = onEditPressed,
            actionType = RowActionIconParams.ActionType.REFINE
        )
    )
}

@Preview("CurrentPageRow, 1x font scale", locale = "en")
@Preview("CurrentPageRow, 2x font scale", locale = "en", fontScale = 2.0f)
@Preview("CurrentPageRow, RTL", locale = "he")
@Composable
fun CurrentPageRowPreview() {
    LightDarkPreviewContainer {
        CurrentPageRow(
            faviconBitmap = null,
            label = "https://www.example.com",
            isShowingQuery = false
        ) {}
    }
}
