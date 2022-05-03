package com.neeva.app.browsing.findinpage

import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.chromium.weblayer.FindInPageCallback
import org.chromium.weblayer.Tab

class FindInPageModelImpl : FindInPageModel {
    private val _findInPageInfo = MutableStateFlow(FindInPageInfo())
    override val findInPageInfoFlow: StateFlow<FindInPageInfo> = _findInPageInfo

    private var activeTab: WeakReference<Tab> = WeakReference(null)

    private val findInPageCallback = object : FindInPageCallback() {
        override fun onFindResult(
            numberOfMatches: Int,
            activeMatchIndex: Int,
            finalUpdate: Boolean
        ) {
            _findInPageInfo.value = findInPageInfoFlow.value.copy(
                activeMatchIndex = activeMatchIndex,
                numberOfMatches = numberOfMatches,
                finalUpdate = finalUpdate
            )
        }

        override fun onFindEnded() {
            activeTab.clear()
            _findInPageInfo.value = FindInPageInfo()
        }
    }

    fun showFindInPage(tab: Tab) {
        this.activeTab = WeakReference(tab)

        val findInPageController = tab.findInPageController
        findInPageController.setFindInPageCallback(findInPageCallback)

        // Don't update Find in Page values if we didn't get a tab.
        _findInPageInfo.value = FindInPageInfo(text = "")
    }

    override fun updateFindInPageQuery(text: String?) {
        // Expect a call to show first as that registers the callback.
        if (!text.isNullOrEmpty() && findInPageInfoFlow.value.text == null) return

        if (text != null) {
            _findInPageInfo.value = findInPageInfoFlow.value.copy(text = text)
            activeTab.get()?.findInPageController?.find(text, true)
        } else {
            _findInPageInfo.value = FindInPageInfo()
            activeTab.get()?.findInPageController?.setFindInPageCallback(null)
        }
    }

    override fun scrollToFindInPageResult(goForward: Boolean) {
        val query = _findInPageInfo.value.text
        if (!query.isNullOrEmpty()) {
            activeTab.get()?.findInPageController?.find(query, goForward)
        }
    }
}

/** For Preview testing. */
class PreviewFindInPageModel : FindInPageModel {
    override val findInPageInfoFlow: StateFlow<FindInPageInfo> = MutableStateFlow(FindInPageInfo())

    override fun updateFindInPageQuery(text: String?) {}

    override fun scrollToFindInPageResult(goForward: Boolean) {}
}
