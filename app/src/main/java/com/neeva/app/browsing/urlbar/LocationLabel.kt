// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing.urlbar

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import com.neeva.app.LocalBrowserToolbarModel
import com.neeva.app.R
import com.neeva.app.browsing.ActiveTabModel
import com.neeva.app.ui.OneBooleanPreviewContainer
import com.neeva.app.ui.PortraitPreviews
import com.neeva.app.ui.reparentView
import com.neeva.app.ui.theme.Dimensions
import com.neeva.app.ui.theme.mapComposeColorToResource
import org.chromium.weblayer.UrlBarOptions

@Composable
fun LocationLabel(
    placeholderColor: Color,
    modifier: Modifier = Modifier
) {
    val browserToolbarModel = LocalBrowserToolbarModel.current
    val displayInfo by browserToolbarModel.displayedInfoFlow.collectAsState()

    // WebLayer requires that you pass it an old-school color resource from the XML files.
    val colorResource = mapComposeColorToResource(LocalContentColor.current)
    val textSize = MaterialTheme.typography.bodyLarge.fontSize.value

    // Whenever the UrlBarController changes, ask it for the View we need to display and recompose.
    val urlBarController by browserToolbarModel.urlBarModel.urlBarControllerFlow
        .collectAsState(null)
    val urlBarView: View? = remember(urlBarController) {
        urlBarController?.createUrlBarView(
            UrlBarOptions.builder()
                .setTextSizeSP(textSize)
                .setIconColor(colorResource)
                .setTextColor(colorResource)
                .build()
        )
    }

    Box {
        HiddenUrlBar()

        LocationLabelContent(
            urlBarView = urlBarView,
            mode = displayInfo.mode,
            displayedText = displayInfo.displayedText,
            placeholderColor = placeholderColor,
            modifier = modifier.semantics { testTag = "LocationLabel" }
        )
    }
}

@Composable
fun LocationLabelContent(
    urlBarView: View?,
    mode: ActiveTabModel.DisplayMode,
    displayedText: String,
    placeholderColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimensions.PADDING_MEDIUM),
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
                    contentDescription = stringResource(R.string.neeva_search),
                    modifier = Modifier
                        .padding(end = Dimensions.PADDING_SMALL)
                        .size(Dimensions.SIZE_ICON_SMALL)
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
                        reparentView(
                            urlBarView,
                            rootView,
                            FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                        )
                    }
                )
            }
        }
    }
}

/**
 * Add an Android TextView that presents the full URL for password managers but is not displayed to
 * users (users see only the URI authority presented by WebLayer via the LocationLabel).
 */
@Composable
fun HiddenUrlBar() {
    val browserToolbarModel = LocalBrowserToolbarModel.current
    val url by browserToolbarModel.urlFlow.collectAsState()

    val context = LocalContext.current

    val fullUrlView: TextView = remember {
        TextView(context).apply {
            id = R.id.full_url_text_view
            visibility = View.GONE
        }
    }

    LaunchedEffect(url) {
        fullUrlView.text = url.toString()
    }

    AndroidView(
        factory = { FrameLayout(it) },
        update = { rootView: FrameLayout ->
            reparentView(
                fullUrlView,
                rootView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
    )
}

@PortraitPreviews
@Composable
private fun URLPreview() {
    OneBooleanPreviewContainer { useLongText ->
        val urlBarText = if (useLongText) {
            stringResource(id = R.string.debug_long_url)
        } else {
            stringResource(id = R.string.debug_short_url)
        }

        Surface(modifier = Modifier.height(dimensionResource(R.dimen.top_toolbar_height))) {
            // Create a proxy for the WebLayer URL bar.
            val weblayerUrlBar = TextView(LocalContext.current)
            weblayerUrlBar.text = urlBarText
            weblayerUrlBar.setTextColor(MaterialTheme.colorScheme.onSurface.toArgb())
            weblayerUrlBar.maxLines = 1
            weblayerUrlBar.textAlignment = TextView.TEXT_ALIGNMENT_CENTER

            LocationLabelContent(
                urlBarView = weblayerUrlBar,
                mode = ActiveTabModel.DisplayMode.URL,
                displayedText = urlBarText,
                placeholderColor = Color.Magenta,
                modifier = Modifier.background(MaterialTheme.colorScheme.background)
            )
        }
    }
}

@PortraitPreviews
@Composable
private fun QueryPreview() {
    OneBooleanPreviewContainer { useLongText ->
        val urlBarText = if (useLongText) {
            stringResource(id = R.string.debug_long_string_primary)
        } else {
            stringResource(id = R.string.debug_short_url)
        }

        Surface(modifier = Modifier.height(dimensionResource(R.dimen.top_toolbar_height))) {
            LocationLabelContent(
                urlBarView = TextView(LocalContext.current),
                mode = ActiveTabModel.DisplayMode.QUERY,
                displayedText = urlBarText,
                placeholderColor = Color.Magenta,
                modifier = Modifier.background(MaterialTheme.colorScheme.background)
            )
        }
    }
}

@PortraitPreviews
@Composable
private fun NeevaHomepagePreview() {
    OneBooleanPreviewContainer { isIncognito ->
        val placeholderColor = if (isIncognito) {
            MaterialTheme.colorScheme.inverseOnSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

        Surface(modifier = Modifier.height(dimensionResource(R.dimen.top_toolbar_height))) {
            LocationLabelContent(
                urlBarView = TextView(LocalContext.current),
                mode = ActiveTabModel.DisplayMode.PLACEHOLDER,
                displayedText = stringResource(id = R.string.url_bar_placeholder),
                placeholderColor = placeholderColor,
                modifier = Modifier.background(MaterialTheme.colorScheme.background)
            )
        }
    }
}
