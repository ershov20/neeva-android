package com.neeva.app.history

import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import com.neeva.app.LocalEnvironment
import com.neeva.app.storage.favicons.FaviconCache

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HistoryContainer(faviconCache: FaviconCache, onOpenUrl: (Uri) -> Unit) {
    val appNavModel = LocalEnvironment.current.appNavModel

    HistoryUI(
        onClose = appNavModel::showBrowser,
        onOpenUrl = onOpenUrl,
        faviconCache = faviconCache
    )
}
