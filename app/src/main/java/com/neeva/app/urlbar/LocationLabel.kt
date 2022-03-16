package com.neeva.app.urlbar

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.neeva_menu.NeevaMenuItemId
import com.neeva.app.neeva_menu.OverflowMenu
import com.neeva.app.ui.BooleanPreviewParameterProvider
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.NeevaTheme
import com.neeva.app.ui.theme.mapComposeColorToResource
import org.chromium.weblayer.UrlBarOptions

@Composable
fun LocationLabel(
    showIncognitoBadge: Boolean,
    onMenuItem: (id: NeevaMenuItemId) -> Unit,
    placeholderColor: Color,
    modifier: Modifier = Modifier
) {
    val browserWrapper = LocalBrowserWrapper.current

    val activeTabModel = browserWrapper.activeTabModel
    val displayedInfo by activeTabModel.displayedInfoFlow.collectAsState()

    // WebLayer requires that you pass it an old-school color resource from the XML files.
    val colorResource = mapComposeColorToResource(LocalContentColor.current)
    val textSize = MaterialTheme.typography.bodyLarge.fontSize.value

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
        mode = displayedInfo.mode,
        displayedText = displayedInfo.displayedText,
        showIncognitoBadge = showIncognitoBadge,
        onMenuItem = onMenuItem,
        placeholderColor = placeholderColor,
        modifier = modifier
    )
}

@Composable
fun LocationLabel(
    urlBarView: View?,
    mode: ActiveTabModel.DisplayMode,
    displayedText: String,
    showIncognitoBadge: Boolean,
    onMenuItem: (id: NeevaMenuItemId) -> Unit,
    placeholderColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(start = Dimensions.PADDING_MEDIUM)
    ) {
        val iconSize = 18.dp
        val iconModifier = Modifier
            .padding(end = Dimensions.PADDING_LARGE)
            .size(iconSize)
        if (showIncognitoBadge) {
            Icon(
                painter = painterResource(R.drawable.ic_incognito),
                contentDescription = stringResource(R.string.incognito),
                modifier = iconModifier
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
            when (mode) {
                ActiveTabModel.DisplayMode.PLACEHOLDER -> {
                    Text(
                        text = stringResource(R.string.url_bar_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        color = placeholderColor,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                ActiveTabModel.DisplayMode.QUERY -> {
                    Icon(
                        painter = painterResource(R.drawable.ic_baseline_search_24),
                        contentDescription = stringResource(R.string.search),
                        modifier = Modifier.padding(end = 8.dp).size(16.dp)
                    )
                    Text(
                        text = displayedText,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                ActiveTabModel.DisplayMode.URL -> {
                    AndroidView(
                        factory = { FrameLayout(it) },
                        update = { rootView: FrameLayout ->
                            if (urlBarView == null || urlBarView.parent == rootView) {
                                return@AndroidView
                            }

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
        }

        OverflowMenu(onMenuItem = onMenuItem)
    }
}

class LocationLabelPreviews : BooleanPreviewParameterProvider<LocationLabelPreviews.Params>(3) {
    data class Params(
        val darkTheme: Boolean,
        val isIncognito: Boolean,
        val useLongText: Boolean
    )

    override fun createParams(booleanArray: BooleanArray) = Params(
        darkTheme = booleanArray[0],
        isIncognito = booleanArray[1],
        useLongText = booleanArray[2]
    )

    @Preview("1x font scale", locale = "en")
    @Preview("2x font scale", locale = "en", fontScale = 2.0f)
    @Preview("RTL, 1x font scale", locale = "he")
    @Composable
    fun URLPreview(@PreviewParameter(LocationLabelPreviews::class) params: Params) {
        val urlBarText = if (params.useLongText) {
            stringResource(id = R.string.debug_long_url)
        } else {
            stringResource(id = R.string.debug_short_url)
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
                mode = ActiveTabModel.DisplayMode.URL,
                displayedText = urlBarText,
                showIncognitoBadge = params.isIncognito,
                onMenuItem = {},
                placeholderColor = Color.Magenta,
                modifier = Modifier.background(MaterialTheme.colorScheme.background)
            )
        }
    }

    @Preview("1x font scale", locale = "en")
    @Preview("2x font scale", locale = "en", fontScale = 2.0f)
    @Preview("RTL, 1x font scale", locale = "he")
    @Composable
    fun QueryPreview(@PreviewParameter(LocationLabelPreviews::class) params: Params) {
        val urlBarText = if (params.useLongText) {
            stringResource(id = R.string.debug_long_string_primary)
        } else {
            stringResource(id = R.string.debug_short_url)
        }

        NeevaTheme(useDarkTheme = params.darkTheme) {
            LocationLabel(
                urlBarView = TextView(LocalContext.current),
                mode = ActiveTabModel.DisplayMode.QUERY,
                displayedText = urlBarText,
                showIncognitoBadge = params.isIncognito,
                onMenuItem = {},
                placeholderColor = Color.Magenta,
                modifier = Modifier.background(MaterialTheme.colorScheme.background)
            )
        }
    }

    @Preview("1x font scale", locale = "en")
    @Preview("2x font scale", locale = "en", fontScale = 2.0f)
    @Composable
    fun NeevaHomepagePreview(@PreviewParameter(LocationLabelPreviews::class) params: Params) {
        val urlBarText = stringResource(id = R.string.url_bar_placeholder)

        NeevaTheme(useDarkTheme = params.darkTheme) {
            Surface {
                LocationLabel(
                    urlBarView = TextView(LocalContext.current),
                    mode = ActiveTabModel.DisplayMode.PLACEHOLDER,
                    displayedText = urlBarText,
                    showIncognitoBadge = params.isIncognito,
                    onMenuItem = {},
                    placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.background(MaterialTheme.colorScheme.background)
                )
            }
        }
    }
}
