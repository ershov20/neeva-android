package com.neeva.app.ui

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.neeva.app.LocalAppNavModel
import com.neeva.app.LocalBrowserToolbarModel
import com.neeva.app.LocalBrowserWrapper
import com.neeva.app.ToolbarConfiguration
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
    val appNavModel = LocalAppNavModel.current
    val browserWrapper by webLayerModel.currentBrowserFlow.collectAsState()
    val urlBarModel = browserWrapper.urlBarModel

    val toolbarConfiguration by toolbarConfigurationFlow.collectAsState()
    val urlBarModelState: URLBarModelState by urlBarModel.stateFlow.collectAsState()
    val isEditing = urlBarModelState.isEditing

    val browserToolbarModel = remember(appNavModel, browserWrapper, toolbarConfiguration) {
        BrowserToolbarModelImpl(
            appNavModel,
            browserWrapper,
            toolbarConfiguration
        )
    }

    CompositionLocalProvider(
        LocalBrowserToolbarModel provides browserToolbarModel,
        LocalBrowserWrapper provides browserWrapper
    ) {
        // Re-parent the WebLayer Fragment's Views directly into the Composable hierarchy.
        // This is a dirty hack that works around WebLayer not playing well with Compose:
        // * https://github.com/neevaco/neeva-android/issues/530
        //
        // * The WebLayer Fragment needs to be inserted using a FragmentTransaction, which requires
        //   using a regular View ID in the Android View hierarchy.  Because Compose doesn't use
        //   Views (and attempts to add an ID to the one created by the AndroidView were unreliably
        //   found using Activity.findViewById()), we have to manually move the WebLayer Fragment's
        //   Views into the FrameLayout that is whenever the BrowserScaffold is recomposed.
        //   More annoyingly, because the BrowserScaffold can be removed from the Composable
        //   hierarchy whenever the user navigates to another screen, the factory lambda will fire
        //   and create a new FrameLayout.
        //
        // * WebLayer has its own mechanism for auto-hiding toolbars that is completely incompatible
        //   with how a Composable Scaffold works.
        AndroidView(
            factory = { FrameLayout(it) },
            modifier = Modifier.fillMaxSize(),
            update = { composeContainer ->
                browserWrapper.getFragment()?.view?.let { reparentView(it, composeContainer) }
            }
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Make sure that the top toolbar is visible if the user is editing the URL.
            BrowserToolbarContainer(
                topOffset = if (isEditing) {
                    0f
                } else {
                    toolbarConfiguration.topControlOffset
                }
            )

            // We have to use a Box with no background because the WebLayer Fragments are displayed
            // in regular Android Views underneath this View in the hierarchy.
            Box(
                modifier = Modifier
                    .weight(1.0f)
                    .fillMaxWidth()
            ) {
                val shouldDisplayCrashedTab
                    by browserWrapper.shouldDisplayCrashedTab.collectAsState(false)

                // Full sized views, drawn below the two toolbars.
                when {
                    isEditing -> {
                        SuggestionPane(modifier = Modifier.fillMaxSize())
                    }

                    shouldDisplayCrashedTab -> {
                        CrashedTab(
                            onReload = browserWrapper::reload,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    else -> {
                        // TODO(dan.alcantara): This is where the WebLayer Views _should_ go, but
                        // WebLayer has a different idea of where its Android Views should live in
                        // the hierarchy and doesn't seem to work well inside of a Composable
                        // Scaffold, which has its own mechanisms for auto-hiding toolbars.
                    }
                }
            }

            if (!isEditing && !browserToolbarModel.useSingleBrowserToolbar) {
                BrowserBottomToolbar(
                    bottomOffset = toolbarConfiguration.bottomControlOffset
                )
            }
        }
    }
}

private fun reparentView(childView: View, desiredParentView: ViewGroup) {
    if (childView.parent == null || childView.parent == desiredParentView) return

    (childView.parent as ViewGroup).removeView(childView)
    desiredParentView.addView(
        childView,
        ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    )
}
