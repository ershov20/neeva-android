package com.neeva.app.urlbar

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.neeva.app.LocalEnvironment
import com.neeva.app.R
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.ui.theme.mapComposeColorToResource
import org.chromium.weblayer.Browser
import org.chromium.weblayer.UrlBarOptions

@Composable
fun LocationLabel(
    urlBarValue: String,
    backgroundColor: Color,
    foregroundColor: Color,
    showIncognitoBadge: Boolean,
    isShowingQuery: Boolean,
    onReload: () -> Unit,
    modifier: Modifier = Modifier
) {
    // WebLayer requires that you pass it an old-school color resource from the XML files.
    val textSize = MaterialTheme.typography.bodyLarge.fontSize.value
    val colorResource = mapComposeColorToResource(foregroundColor)

    // Whenever the Browser changes, ask it for the View we need to display and trigger a recompose.
    val browserWrapper = LocalEnvironment.current.browserWrapper
    val browserFlow: Browser? by browserWrapper.browserFlow.collectAsState()
    val urlBarView: View? = remember(browserFlow) {
        browserFlow?.urlBarController?.createUrlBarView(
            UrlBarOptions.builder()
                .setTextSizeSP(textSize)
                .setIconColor(colorResource)
                .setTextColor(colorResource)
                .build()
        )
    }

    LocationLabel(
        urlBarView,
        urlBarValue,
        backgroundColor,
        foregroundColor,
        showIncognitoBadge,
        isShowingQuery,
        onReload,
        modifier
    )
}

@Composable
fun LocationLabel(
    urlBarView: View?,
    urlBarValue: String,
    backgroundColor: Color,
    foregroundColor: Color,
    showIncognitoBadge: Boolean,
    isShowingQuery: Boolean,
    onReload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(backgroundColor)
            .wrapContentSize(Alignment.Center)
            .defaultMinSize(minHeight = 40.dp)
    ) {
        val iconSize = 24.dp
        val iconModifier = Modifier
            .padding(8.dp)
            .size(iconSize)
        if (showIncognitoBadge) {
            Image(
                painter = painterResource(R.drawable.ic_incognito),
                contentDescription = stringResource(R.string.incognito),
                modifier = iconModifier,
                colorFilter = ColorFilter.tint(foregroundColor)
            )
        } else {
            Box(modifier = iconModifier)
        }

        Spacer(modifier = Modifier.weight(1.0f))

        if (isShowingQuery) {
            Image(
                painter = painterResource(R.drawable.ic_baseline_search_24),
                contentDescription = "secure site",
                modifier = Modifier
                    .padding(8.dp)
                    .size(16.dp),
                colorFilter = ColorFilter.tint(foregroundColor),
                contentScale = ContentScale.Fit
            )
            Text(
                text = urlBarValue.ifEmpty { "Search or enter address" },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                color = foregroundColor
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

        Spacer(modifier = Modifier.weight(1.0f))
    }
}

class LocationLabelPreviews : BooleanPreviewParameterProvider<LocationLabelPreviews.Params>(3) {
    data class Params(
        val darkTheme: Boolean,
        val isIncognito: Boolean,
        val isShowingQuery: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0],
        isIncognito = booleanArray[1],
        isShowingQuery = booleanArray[2]
    )

    @Preview("1x font scale", locale = "en")
    @Preview("2x font scale", locale = "en", fontScale = 2.0f)
    @Preview("RTL, 1x font scale", locale = "he")
    @Preview("RTL, 2x font scale", locale = "he", fontScale = 2.0f)
    @Composable
    fun LocationLabelPreview(
        @PreviewParameter(LocationLabelPreviews::class) params: Params
    ) {
        NeevaTheme(useDarkTheme = params.darkTheme) {
            // Create a proxy for the Weblayer URL bar.
            val urlBarView = TextView(LocalContext.current)
            urlBarView.text = stringResource(id = R.string.debug_short_url)
            urlBarView.setTextColor(MaterialTheme.colorScheme.onSurface.toArgb())
            urlBarView.maxLines = 1
            urlBarView.textAlignment = TextView.TEXT_ALIGNMENT_CENTER

            LocationLabel(
                urlBarView = urlBarView,
                urlBarValue = stringResource(id = R.string.debug_short_url),
                backgroundColor = MaterialTheme.colorScheme.background,
                foregroundColor = MaterialTheme.colorScheme.onSurface,
                showIncognitoBadge = params.isIncognito,
                isShowingQuery = params.isShowingQuery,
                onReload = {}
            )
        }
    }
}
