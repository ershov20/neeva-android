package com.neeva.app.browsing.urlbar

import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.chromium.weblayer.UrlBarController

/** For Previews only. */
class PreviewUrlBarModel(urlBarModelState: URLBarModelState) : URLBarModel {
    override val stateFlow: StateFlow<URLBarModelState> = MutableStateFlow(urlBarModelState)
    override val urlBarControllerFlow: Flow<UrlBarController?> = MutableStateFlow(null)

    override fun replaceLocationBarText(newValue: String, isRefining: Boolean) {}
    override fun onLocationBarTextChanged(newValue: TextFieldValue) {}
    override fun acceptAutocompleteSuggestion() {}
    override fun showZeroQuery(focusUrlBar: Boolean, isLazyTab: Boolean) {}
    override fun clearFocus() {}
}
