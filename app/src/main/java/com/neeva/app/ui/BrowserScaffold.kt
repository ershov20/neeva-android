// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.ui

import android.widget.FrameLayout
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalBrowserToolbarModel
import com.neeva.app.LocalBrowserWrapper
import com.neeva.app.LocalNeevaConstants
import com.neeva.app.R
import com.neeva.app.ToolbarConfiguration
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.browsing.toolbar.BrowserBottomToolbar
import com.neeva.app.browsing.toolbar.BrowserToolbarContainer
import com.neeva.app.browsing.toolbar.BrowserToolbarModelImpl
import com.neeva.app.browsing.urlbar.URLBarModelState
import com.neeva.app.suggestions.SuggestionPane
import com.neeva.app.ui.theme.Dimensions
import java.lang.Float.min
import kotlinx.coroutines.flow.StateFlow

@Composable
fun BrowserScaffold(
    toolbarConfigurationFlow: StateFlow<ToolbarConfiguration>,
    webLayerModel: WebLayerModel
) {
    val browsers by webLayerModel.browsersFlow.collectAsState()
    val browserWrapper = browsers.getCurrentBrowser()
    Box(modifier = Modifier.fillMaxSize()) {
        WebLayerContainer(browserWrapper)
        PullToRefreshBox(browserWrapper)
        BrowserOverlay(toolbarConfigurationFlow, browserWrapper)
    }
}

/**
 * Re-parent the WebLayer Fragment's Views directly into the Composable hierarchy
 * underneath all of the other browser-related UI.
 * This is a dirty hack that works around WebLayer not playing well with Compose:
 * - https://github.com/neevaco/neeva-android/issues/530
 *
 * - The WebLayer Fragment needs to be inserted using a FragmentTransaction, which
 *   requires using a regular View ID in the Android View hierarchy.  Because Compose
 *   doesn't use Views (and attempts to add an ID to the one created by the AndroidView
 *   were unreliably found using Activity.findViewById()), we have to manually move the
 *   WebLayer Fragment's Views into the FrameLayout that is whenever the BrowserScaffold
 *   is recomposed.  More annoyingly, because the BrowserScaffold can be removed from
 *   the Composable hierarchy whenever the user navigates to another screen, the factory
 *   lambda will fire and create a new FrameLayout.
 *
 * - WebLayer has its own mechanism for auto-hiding toolbars that is completely
 *   incompatible with how a Composable Scaffold works.
 */
@Composable
private fun WebLayerContainer(browserWrapper: BrowserWrapper) {
    val currentEvent by browserWrapper.fragmentViewLifecycleEventFlow.collectAsState()
    AndroidView(
        factory = {
            FrameLayout(it).apply {
                id = R.id.weblayer_fragment_view_container
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTag = "WebLayerContainer" },
        update = { composeContainer ->
            // Force Compose to update this Composable whenever the state of the Fragment's View
            // says that it _should_ be visible.
            if (currentEvent == Lifecycle.Event.ON_RESUME) {
                browserWrapper.getFragment()?.view?.let {
                    reparentView(it, composeContainer)
                }
            }
        }
    )
}

@Composable
fun PullToRefreshBox(browserWrapper: BrowserWrapper) {
    val verticalOverscroll by browserWrapper.activeTabModel.verticalOverscrollFlow.collectAsState()
    PullToRefreshBox(verticalOverscroll = verticalOverscroll)
}

@Composable
fun PullToRefreshBox(verticalOverscroll: Float) {
    val shouldDisplayPullToRefresh by remember(verticalOverscroll) {
        derivedStateOf {
            verticalOverscroll < 0
        }
    }

    if (shouldDisplayPullToRefresh) {
        val backgroundColor = MaterialTheme.colorScheme.primary

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(
                    x = 0.dp,
                    y = min(-(verticalOverscroll / 10).dp, Dimensions.SIZE_TOUCH_TARGET * 2.5f)
                )
        ) {

            Spacer(modifier = Modifier.weight(1.0f))

            Surface(
                color = backgroundColor,
                modifier = Modifier
                    .size(Dimensions.SIZE_TOUCH_TARGET)
                    .rotate(min(-verticalOverscroll / 5, 480f)),
                shape = CircleShape,
                shadowElevation = 4.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Icon(
                        modifier = Modifier
                            .padding(Dimensions.PADDING_MEDIUM)
                            .size(Dimensions.SIZE_ICON),
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary
                    )

                    Surface(
                        color = Color.Transparent,
                        modifier = Modifier
                            .padding(Dimensions.PADDING_MEDIUM)
                            .size(Dimensions.SIZE_ICON)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(
                                color = backgroundColor,
                                min(-verticalOverscroll / 2.5f, 360f),
                                360f - min(-verticalOverscroll / 2.5f, 360f),
                                true,
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1.0f))
        }
    }
}

/** Full sized views that cover the browser, sandwiched between the toolbars. */
@Composable
private fun BoxScope.BrowserOverlay(
    toolbarConfigurationFlow: StateFlow<ToolbarConfiguration>,
    browserWrapper: BrowserWrapper
) {
    val shouldDisplayCrashedTab by browserWrapper.shouldDisplayCrashedTab.collectAsState(false)

    val appNavModel = LocalAppNavModel.current
    val urlBarModel = browserWrapper.urlBarModel
    val neevaConstants = LocalNeevaConstants.current

    val toolbarConfiguration by toolbarConfigurationFlow.collectAsState()
    val urlBarModelState: URLBarModelState by urlBarModel.stateFlow.collectAsState()
    val isEditing = urlBarModelState.isEditing
    val focusUrlBar = urlBarModelState.focusUrlBar

    val browserToolbarModel = remember(appNavModel, browserWrapper, toolbarConfiguration) {
        BrowserToolbarModelImpl(
            appNavModel,
            browserWrapper,
            toolbarConfiguration,
            neevaConstants
        )
    }

    val showBottomBar = when {
        // The user is typing something.  Hide the bottom bar to give them more room.
        toolbarConfiguration.isKeyboardOpen -> false

        // The user is in either Zero Query or in the suggestions pane.
        isEditing -> false

        // The user is in landscape.
        browserToolbarModel.useSingleBrowserToolbar -> false

        else -> true
    }

    CompositionLocalProvider(
        LocalBrowserToolbarModel provides browserToolbarModel,
        LocalBrowserWrapper provides browserWrapper
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            // Make sure that the top toolbar is visible if the user is editing the URL.
            BrowserToolbarContainer(
                topOffset = if (isEditing) {
                    0f
                } else {
                    toolbarConfiguration.topControlOffset
                }
            )

            // The Column will grow to take all the available space when suggestions (e.g.) are
            // shown.  Otherwise, the Column will wrap only the toolbar's height.  This allows a gap
            // for the Composable containing the WebLayer's Fragment's View.
            val childModifier = Modifier
                .weight(1.0f)
                .fillMaxWidth()
                .then(
                    if (showBottomBar) {
                        // If the bottom bar is visible, add some padding so that the contents of
                        // the Child view don't get hidden behind it.
                        Modifier.padding(bottom = dimensionResource(R.dimen.bottom_toolbar_height))
                    } else {
                        Modifier
                    }
                )
            when {
                isEditing -> {
                    // Right now, the URL bar is unfocused
                    // while showing Zero Query only for first run.
                    SuggestionPane(modifier = childModifier, isFirstRun = !focusUrlBar)
                }

                shouldDisplayCrashedTab -> {
                    CrashedTab(
                        onReload = browserWrapper::reload,
                        modifier = childModifier
                    )
                }
            }
        }

        // This sits outside of the Column so TalkBack can focus the WebLayer's Fragment's View.
        if (showBottomBar) {
            BrowserBottomToolbar(
                bottomOffset = toolbarConfiguration.bottomControlOffset,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@Preview
@Composable
fun PullToRefreshBox_Preview() {
    LightDarkPreviewContainer {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.height(160.dp).fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                PullToRefreshBox(verticalOverscroll = -0.0f)
            }
            Column(modifier = Modifier.weight(1f)) {
                PullToRefreshBox(verticalOverscroll = -200.0f)
            }
            Column(modifier = Modifier.weight(1f)) {
                PullToRefreshBox(verticalOverscroll = -400.0f)
            }
            Column(modifier = Modifier.weight(1f)) {
                PullToRefreshBox(verticalOverscroll = -600.0f)
            }
            Column(modifier = Modifier.weight(1f)) {
                PullToRefreshBox(verticalOverscroll = -800.0f)
            }
            Column(modifier = Modifier.weight(1f)) {
                PullToRefreshBox(verticalOverscroll = -1000.0f)
            }
        }
    }
}
