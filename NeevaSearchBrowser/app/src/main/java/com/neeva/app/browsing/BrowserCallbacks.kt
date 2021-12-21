package com.neeva.app.browsing

import android.graphics.Point
import org.chromium.weblayer.ContextMenuParams
import org.chromium.weblayer.Tab

/** Callbacks that are necessary when interacting with web content. */
interface BrowserCallbacks {
    /**
     * Called when a browser tab asks the app to enter fullscreen mode.
     * @return The window flags that were set before the user was sent into fullscreen mode.
     */
    fun onEnterFullscreen(): Int

    /**
     * Called when a browser tab asks the app to exit fullscreen mode.
     * @param systemVisibilityToRestore The window flags that were set before the user was sent into fullscreen mode.
     */
    fun onExitFullscreen(systemVisibilityToRestore: Int)

    /** Called when a browser tab wants the app to be brought to the foreground. */
    fun bringToForeground()

    /** Shows the context menu for a link. */
    fun showContextMenuForTab(contextMenuParams: ContextMenuParams, tab: Tab)

    /** Returns the display size to use for the browser. */
    fun getDisplaySize(): Point

    /** Called when the bottom bar needs to be translated from its current location as the user scrolls. */
    fun onBottomBarOffsetChanged(offset: Int)
}