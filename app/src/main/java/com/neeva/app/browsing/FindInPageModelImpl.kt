package com.neeva.app.browsing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.chromium.weblayer.FindInPageCallback
import org.chromium.weblayer.Tab

class FindInPageModelImpl : FindInPageModel {
    private val _findInPageInfo = MutableStateFlow(FindInPageInfo())
    override val findInPageInfo: StateFlow<FindInPageInfo> = _findInPageInfo

    private val findInPageCallback = object : FindInPageCallback() {
        override fun onFindResult(
            numberOfMatches: Int,
            activeMatchIndex: Int,
            finalUpdate: Boolean
        ) {
            _findInPageInfo.value = findInPageInfo.value.copy(
                activeMatchIndex = activeMatchIndex,
                numberOfMatches = numberOfMatches,
                finalUpdate = finalUpdate
            )
        }

        override fun onFindEnded() {
            _findInPageInfo.value = FindInPageInfo()
        }
    }

    fun showFindInPage(tab: Tab?) {
        val findInPageController = tab?.findInPageController ?: return
        findInPageController.setFindInPageCallback(findInPageCallback)

        // Don't update Find in Page values if we didn't get a tab.
        _findInPageInfo.value = FindInPageInfo(text = "")
    }

    fun updateFindInPageQuery(tab: Tab?, text: String?) {
        // Expect a call to show first as that registers the callback
        if (!text.isNullOrEmpty() && findInPageInfo.value.text == null) return

        _findInPageInfo.value = findInPageInfo.value.copy(text = text)
        if (text != null) {
            tab?.findInPageController?.find(text, true)
        } else {
            _findInPageInfo.value = FindInPageInfo()
            tab?.findInPageController?.setFindInPageCallback(null)
        }
    }

    fun scrollToFindInPageResult(tab: Tab?, goForward: Boolean) {
        val query = _findInPageInfo.value.text
        if (!query.isNullOrEmpty()) {
            tab?.findInPageController?.find(query, goForward)
        }
    }
}
