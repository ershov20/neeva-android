package com.neeva.app.browsing

import android.graphics.Rect
import org.chromium.weblayer.ContextMenuParams
import org.chromium.weblayer.Tab

/** Callbacks that are necessary when interacting with web content. */
interface ActivityCallbacks {
    /** Called when a browser tab asks the app to enter fullscreen mode. */
    fun onEnterFullscreen()

    /** Called when a browser tab asks the app to exit fullscreen mode. */
    fun onExitFullscreen()

    /** Called when a browser tab wants the app to be brought to the foreground. */
    fun bringToForeground()

    /** Shows the context menu for a link. */
    fun showContextMenuForTab(contextMenuParams: ContextMenuParams, tab: Tab)

    /** Returns the display size to use for the browser. */
    fun getDisplaySize(): Rect

    /** Called when the bottom bar needs to be translated from its current location as the user scrolls. */
    fun onBottomBarOffsetChanged(offset: Int)

    /** Called when the top bar needs to be translated from its current location as the user scrolls. */
    fun onTopBarOffsetChanged(offset: Int)

    /** Called when the toolbars offset needs to be reset to 0 (completely visible) */
    fun resetToolbarOffset() {
        onTopBarOffsetChanged(0)
        onBottomBarOffsetChanged(0)
    }

    /**
     * Called when the Fragment containing the Incognito profile should be removed from the
     * hierarchy, allowing it to be culled and deleted by WebLayer.
     */
    fun detachIncognitoFragment()

    /** Triggers the logic for the OS-level back button/gesture. */
    fun onBackPressed()
}
