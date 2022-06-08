package com.neeva.app.ui

import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalBrowserToolbarModel
import com.neeva.app.LocalBrowserWrapper
import com.neeva.app.LocalEnvironment
import com.neeva.app.R
import com.neeva.app.ToolbarConfiguration
import com.neeva.app.browsing.BrowserWrapper
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.browsing.toolbar.BrowserBottomToolbar
import com.neeva.app.browsing.toolbar.BrowserToolbarContainer
import com.neeva.app.browsing.toolbar.BrowserToolbarModelImpl
import com.neeva.app.browsing.urlbar.URLBarModelState
import com.neeva.app.suggestions.SuggestionPane
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

/** Full sized views that cover the browser, sandwiched between the toolbars. */
@Composable
private fun BoxScope.BrowserOverlay(
    toolbarConfigurationFlow: StateFlow<ToolbarConfiguration>,
    browserWrapper: BrowserWrapper
) {
    val shouldDisplayCrashedTab by browserWrapper.shouldDisplayCrashedTab.collectAsState(false)

    val appNavModel = LocalAppNavModel.current
    val urlBarModel = browserWrapper.urlBarModel
    val neevaConstants = LocalEnvironment.current.neevaConstants

    val toolbarConfiguration by toolbarConfigurationFlow.collectAsState()
    val urlBarModelState: URLBarModelState by urlBarModel.stateFlow.collectAsState()
    val isEditing = urlBarModelState.isEditing

    val browserToolbarModel = remember(appNavModel, browserWrapper, toolbarConfiguration) {
        BrowserToolbarModelImpl(
            appNavModel,
            browserWrapper,
            toolbarConfiguration,
            neevaConstants
        )
    }

    val showBottomBar by derivedStateOf {
        when {
            // The user is typing something.  Hide the bottom bar to give them more room.
            toolbarConfiguration.isKeyboardOpen -> false

            // The user is in either Zero Query or in the suggestions pane.
            isEditing -> false

            // The user is in landscape.
            browserToolbarModel.useSingleBrowserToolbar -> false

            else -> true
        }
    }

    CompositionLocalProvider(
        LocalBrowserToolbarModel provides browserToolbarModel,
        LocalBrowserWrapper provides browserWrapper
    ) {
        Column(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)) {
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
            val childModifier = Modifier.weight(1.0f).fillMaxWidth()
            when {
                isEditing -> {
                    SuggestionPane(modifier = childModifier)
                }

                shouldDisplayCrashedTab -> {
                    CrashedTab(
                        onReload = browserWrapper::reload,
                        modifier = childModifier
                    )
                }
            }
        }

        if (showBottomBar) {
            BrowserBottomToolbar(
                bottomOffset = toolbarConfiguration.bottomControlOffset,
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)
            )
        }
    }
}
