// Copyright 2022 Neeva Inc. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package com.neeva.app.browsing

import org.chromium.weblayer.FullscreenCallback

class FullscreenCallbackImpl(
    private val activityEnterFullscreen: () -> Unit,
    private val activityExitFullscreen: () -> Unit
) : FullscreenCallback() {
    private var exitRunnable: Runnable? = null

    override fun onEnterFullscreen(exitFullscreenRunnable: Runnable) {
        exitRunnable = exitFullscreenRunnable
        activityEnterFullscreen()
    }

    override fun onExitFullscreen() {
        exitRunnable = null
        activityExitFullscreen()
    }

    fun isFullscreen() = exitRunnable != null

    /**
     * Exits fullscreen if the user is currently in it.
     * @return True if we asked WebLayer to exit fullscreen, false otherwise.
     */
    fun exitFullscreen(): Boolean {
        exitRunnable?.let {
            it.run()
            exitRunnable = null
            return true
        }

        return false
    }
}
