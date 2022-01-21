package com.neeva.app.browsing

import androidx.activity.OnBackPressedCallback
import org.chromium.weblayer.FullscreenCallback

class FullscreenCallbackImpl(
    private val onEnterFullscreen: (OnBackPressedCallback) -> Int?,
    private val onExitFullscreen: (Int) -> Unit
) : FullscreenCallback() {
    private var systemVisibilityToRestore = 0
    private var exitRunnable: Runnable? = null
    private var onBackPressedCallback: OnBackPressedCallback? = null

    override fun onEnterFullscreen(exitFullscreenRunnable: Runnable) {
        val newCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                exitFullscreenRunnable.run()
            }
        }

        onBackPressedCallback = newCallback
        exitRunnable = exitFullscreenRunnable

        onEnterFullscreen(newCallback)?.let {
            systemVisibilityToRestore = it
        }
    }

    override fun onExitFullscreen() {
        onBackPressedCallback?.remove()
        onBackPressedCallback = null
        exitRunnable = null

        onExitFullscreen(systemVisibilityToRestore)
    }
}
