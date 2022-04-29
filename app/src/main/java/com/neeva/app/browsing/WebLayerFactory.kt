package com.neeva.app.browsing

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.chromium.weblayer.Callback
import org.chromium.weblayer.WebLayer

class WebLayerFactory(@ApplicationContext private val appContext: Context) {
    fun load(callback: Callback<WebLayer>) = WebLayer.loadAsync(appContext, callback)
}
