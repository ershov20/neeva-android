package com.neeva.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.neeva.app.BottomToolbar
import com.neeva.app.LocalBrowserWrapper
import com.neeva.app.TopToolbar
import com.neeva.app.browsing.WebLayerModel
import com.neeva.app.suggestions.SuggestionPane
import kotlinx.coroutines.flow.StateFlow

@Composable
fun BrowserScaffold(
    bottomControlOffset: StateFlow<Float>,
    topControlOffset: StateFlow<Float>,
    webLayerModel: WebLayerModel
) {
    val browserWrapper by webLayerModel.currentBrowserFlow.collectAsState()
    val urlBarModel = browserWrapper.urlBarModel

    val topOffset by topControlOffset.collectAsState()
    val bottomOffset by bottomControlOffset.collectAsState()

    val isEditing: Boolean by urlBarModel.isEditing.collectAsState(false)

    CompositionLocalProvider(LocalBrowserWrapper provides browserWrapper) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Make sure that the top toolbar is visible if the user is editing the URL.
            TopToolbar(if (isEditing) 0f else topOffset)

            // We have to use a Box with no background because the WebLayer Fragments are displayed in
            // regular Android Views underneath this View in the hierarchy.
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

            if (!isEditing) {
                BottomToolbar(bottomOffset)
            }
        }
    }
}
