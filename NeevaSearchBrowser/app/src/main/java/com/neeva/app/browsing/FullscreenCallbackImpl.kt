package com.neeva.app.browsing

import org.chromium.weblayer.FullscreenCallback

class FullscreenCallbackImpl(
    private val onEnterFullscreen: () -> Int?,
    private val onExitFullscreen: (Int) -> Unit
) : FullscreenCallback() {
    private var systemVisibilityToRestore = 0
    private var exitFullscreenRunnable: Runnable? = null

    override fun onEnterFullscreen(exitFullscreenRunnable: Runnable) {
        this.exitFullscreenRunnable = exitFullscreenRunnable
        onEnterFullscreen()?.let {
            systemVisibilityToRestore = it
        }
    }

    override fun onExitFullscreen() {
        this.exitFullscreenRunnable = null
        onExitFullscreen(systemVisibilityToRestore)
    }

    fun canExitFullscreen() = exitFullscreenRunnable != null

    fun exitFullscreen() = exitFullscreenRunnable?.run()
}