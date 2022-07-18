package com.neeva.app.suggestions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.neeva.app.LocalPopupModel
import com.neeva.app.R
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.ui.LightDarkPreviewContainer
import com.neeva.app.ui.PopupModel
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.widgets.RowActionIconParams
import com.neeva.app.ui.widgets.RowActionStartIconParams

@Composable
fun CurrentPageRow(browserWrapper: BrowserWrapper) {
    val popupModel: PopupModel = LocalPopupModel.current

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

    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

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
                isShowingQuery = isShowingQuery,
                onEditPressed = {
                    urlBarModel.replaceLocationBarText(
                        if (isShowingQuery) displayedText else currentURL.toString()
                    )
                },
                onCopyPressed = clipboardManager?.let {
                    {
                        it.apply {
                            setPrimaryClip(ClipData.newPlainText("address", currentURL.toString()))
                        }
                        popupModel.showSnackbar(
                            message = context.getString(R.string.copied_to_clipboard)
                        )
                        urlBarModel.clearFocus()
                    }
                }
            )

            SuggestionDivider()
        }
    }
}

@Composable
fun CurrentPageRow(
    faviconBitmap: Bitmap?,
    label: String,
    isShowingQuery: Boolean,
    onEditPressed: () -> Unit,
    onCopyPressed: (() -> Unit)?
) {
    NavSuggestionRow(
        iconParams = RowActionStartIconParams(
            faviconBitmap = faviconBitmap
        ),
        primaryLabel = stringResource(
            id = if (isShowingQuery) {
                R.string.edit_current_search
            } else {
                R.string.edit_current_url
            }
        ),
        onTapRow = onEditPressed,
        secondaryLabel = label,
        actionIconParams = onCopyPressed?.let {
            RowActionIconParams(
                onTapAction = it,
                actionType = RowActionIconParams.ActionType.COPY,
                contentDescription = stringResource(R.string.copy_link)
            )
        },
        showActualUrlInSecondaryLabel = true
    )
}

@PortraitPreviews
@Composable
fun CurrentPageRowPreview() {
    LightDarkPreviewContainer {
        CurrentPageRow(
            faviconBitmap = null,
            label = "https://www.example.com",
            isShowingQuery = false,
            onEditPressed = {},
            onCopyPressed = {}
        )
    }
}
