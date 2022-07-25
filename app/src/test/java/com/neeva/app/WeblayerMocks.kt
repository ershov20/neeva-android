package com.neeva.app

import android.net.Uri
import org.chromium.weblayer.Navigation
import org.chromium.weblayer.NavigationCallback
import org.chromium.weblayer.NavigationController
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

/** Creates a mock [NavigationController] that keeps track of a Tab's navigation history. */
fun createMockNavigationController(): NavigationController {
    val navigatedUrls: MutableList<Uri> = mutableListOf()
    val callbacks = mutableListOf<NavigationCallback>()
    var currentIndex = -1

    fun recordNavigation(uri: Uri) {
        val navigation = mock<Navigation> {
            on { getUri() } doReturn uri
        }

        // Iterate on a copy to avoid a ConcurrentModificationException.
        callbacks.toSet().forEach { callback -> callback.onNavigationStarted(navigation) }

        while (currentIndex + 1 < navigatedUrls.size) { navigatedUrls.removeLast() }
        currentIndex++
        navigatedUrls.add(uri)

        // Iterate on a copy to avoid a ConcurrentModificationException.
        callbacks.toSet().forEach { callback -> callback.onNavigationCompleted(navigation) }
    }

    return mock {
        on { navigationListCurrentIndex } doAnswer { currentIndex }
        on { navigationListSize } doAnswer { navigatedUrls.size }

        on { getNavigationEntryDisplayUri(any()) } doAnswer {
            val index = it.arguments[0] as Int
            navigatedUrls[index]
        }

        on { goBack() } doAnswer {
            if (currentIndex < 0) throw IllegalStateException()
            currentIndex -= 1
        }

        on { goForward() } doAnswer {
            if (currentIndex >= navigatedUrls.size) throw IllegalStateException()
            currentIndex += 1
        }

        on { canGoBack() } doAnswer { currentIndex > 0 }
        on { canGoForward() } doAnswer { currentIndex < navigatedUrls.size - 1 }

        on { navigate(any()) } doAnswer { recordNavigation(it.arguments[0] as Uri) }
        on { navigate(any(), any()) } doAnswer { recordNavigation(it.arguments[0] as Uri) }

        on { registerNavigationCallback(any()) } doAnswer {
            callbacks.add(it.arguments[0] as NavigationCallback)
            Unit
        }
        on { unregisterNavigationCallback(any()) } doAnswer {
            callbacks.remove(it.arguments[0] as NavigationCallback)
            Unit
        }
    }
}
