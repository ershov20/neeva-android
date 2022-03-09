package com.neeva.app.urlbar

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.neeva.app.LocalBrowserWrapper
import com.neeva.app.R
import com.neeva.app.neeva_menu.NeevaMenuItemId
import com.neeva.app.neeva_menu.OverflowMenu
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.ui.theme.mapComposeColorToResource
import org.chromium.weblayer.UrlBarOptions

@Composable
fun LocationLabel(
    foregroundColor: Color,
    showIncognitoBadge: Boolean,
    onMenuItem: (id: NeevaMenuItemId) -> Unit,
    onEditUrl: () -> Unit,
    modifier: Modifier = Modifier
) {
    val browserWrapper = LocalBrowserWrapper.current

    val activeTabModel = browserWrapper.activeTabModel
    val urlBarValue by activeTabModel.displayedText.collectAsState()
    val isShowingQuery by activeTabModel.isShowingQuery.collectAsState()

    // WebLayer requires that you pass it an old-school color resource from the XML files.
    val textSize = MaterialTheme.typography.bodyLarge.fontSize.value
    val colorResource = mapComposeColorToResource(foregroundColor)

    // Whenever the UrlBarController changes, ask it for the View we need to display and recompose.
    val urlBarController by browserWrapper.urlBarControllerFlow.collectAsState(null)
    val urlBarView: View? = remember(urlBarController) {
        urlBarController?.createUrlBarView(
            UrlBarOptions.builder()
                .setTextSizeSP(textSize)
                .setIconColor(colorResource)
                .setTextColor(colorResource)
                .build()
        )
    }

    LocationLabel(
        urlBarView = urlBarView,
        urlBarValue = urlBarValue,
        foregroundColor = foregroundColor,
        showIncognitoBadge = showIncognitoBadge,
        isShowingQuery = isShowingQuery,
        onMenuItem = onMenuItem,
        onEditUrl = onEditUrl,
        modifier = modifier
    )
}

@Composable
fun LocationLabel(
    urlBarView: View?,
    urlBarValue: String,
    foregroundColor: Color,
    showIncognitoBadge: Boolean,
    isShowingQuery: Boolean,
    onMenuItem: (id: NeevaMenuItemId) -> Unit,
    onEditUrl: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable { onEditUrl() }
    ) {
        val iconSize = 18.dp
        val iconModifier = Modifier.padding(end = Dimensions.PADDING_LARGE).size(iconSize)
        if (showIncognitoBadge) {
            Icon(
                painter = painterResource(R.drawable.ic_incognito),
                contentDescription = stringResource(R.string.incognito),
                modifier = iconModifier,
                tint = foregroundColor
            )
        } else {
            // Add a spacer that has the same size as the optional icon so that the URL and query
            // text are properly centered.
            Spacer(modifier = iconModifier)
        }

        Row(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxHeight(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isShowingQuery) {
                Image(
                    painter = painterResource(R.drawable.ic_baseline_search_24),
                    contentDescription = stringResource(R.string.search),
                    modifier = Modifier
                        .padding(8.dp)
                        .size(16.dp),
                    colorFilter = ColorFilter.tint(foregroundColor),
                    contentScale = ContentScale.Fit
                )
                Text(
                    text = urlBarValue,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    color = foregroundColor,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                AndroidView(
                    factory = { FrameLayout(it) },
                    update = { rootView: FrameLayout ->
                        if (urlBarView == null || urlBarView.parent == rootView) return@AndroidView

                        // Attach the URL Bar provided by WebLayer to our hierarchy.
                        rootView.removeAllViews()
                        urlBarView.run {
                            (parent as? ViewGroup)?.removeView(this)
                            rootView.addView(
                                this,
                                FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT
                                )
                            )
                        }
                    }
                )
            }
        }

        OverflowMenu(
            onMenuItem = onMenuItem,
            foregroundColor = foregroundColor
        )
    }
}

class LocationLabelPreviews : BooleanPreviewParameterProvider<LocationLabelPreviews.Params>(4) {
    data class Params(
        val darkTheme: Boolean,
        val isIncognito: Boolean,
        val isShowingQuery: Boolean,
        val useLongText: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0],
        isIncognito = booleanArray[1],
        isShowingQuery = booleanArray[2],
        useLongText = booleanArray[3]
    )

    @Preview("1x font scale", locale = "en")
    @Preview("2x font scale", locale = "en", fontScale = 2.0f)
    @Preview("RTL, 1x font scale", locale = "he")
    @Preview("RTL, 2x font scale", locale = "he", fontScale = 2.0f)
    @Composable
    fun LocationLabelPreview(
        @PreviewParameter(LocationLabelPreviews::class) params: Params
    ) {
        val urlBarText = if (params.isShowingQuery) {
            if (params.useLongText) {
                stringResource(id = R.string.debug_long_string_primary)
            } else {
                stringResource(id = R.string.debug_short_url)
            }
        } else {
            if (params.useLongText) {
                stringResource(id = R.string.debug_long_url)
            } else {
                stringResource(id = R.string.debug_short_url)
            }
        }

        NeevaTheme(useDarkTheme = params.darkTheme) {
            // Create a proxy for the Weblayer URL bar.
            val weblayerUrlBar = TextView(LocalContext.current)
            weblayerUrlBar.text = urlBarText
            weblayerUrlBar.setTextColor(MaterialTheme.colorScheme.onSurface.toArgb())
            weblayerUrlBar.maxLines = 1
            weblayerUrlBar.textAlignment = TextView.TEXT_ALIGNMENT_CENTER

            LocationLabel(
                urlBarView = weblayerUrlBar,
                urlBarValue = urlBarText,
                foregroundColor = MaterialTheme.colorScheme.onSurface,
                showIncognitoBadge = params.isIncognito,
                isShowingQuery = params.isShowingQuery,
                onMenuItem = {},
                onEditUrl = {},
                modifier = Modifier.background(MaterialTheme.colorScheme.background)
            )
        }
    }
}
