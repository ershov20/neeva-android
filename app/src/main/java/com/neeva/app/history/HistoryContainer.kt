package com.neeva.app.history

import android.net.Uri
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.neeva.app.AppNavState
import com.neeva.app.storage.FaviconCache

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HistoryContainer(
    navController: NavController,
    faviconCache: FaviconCache,
    onOpenUrl: (Uri) -> Unit
) {
    HistoryUI(
        onClose = { navController.navigate(AppNavState.BROWSER.name) },
        onOpenUrl = onOpenUrl,
        faviconCache = faviconCache
    )
}
