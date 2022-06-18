package com.neeva.app.browsing

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.neeva.app.R
import com.neeva.app.ui.widgets.menu.MenuContent
import com.neeva.app.ui.widgets.menu.MenuRowData
import org.chromium.weblayer.ContextMenuParams
import org.chromium.weblayer.Tab

/** Context menu that is triggered by long-pressing on a link. */
@Composable
fun LinkContextMenu(
    webLayerModel: WebLayerModel,
    params: ContextMenuParams,
    tab: Tab,
    onDismissRequested: () -> Unit
) {
    val isCurrentTabIncognito: Boolean = webLayerModel.currentBrowser.isIncognito

    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    val sendToClipboard = { label: String, text: String ->
        clipboardManager?.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    val menuItems = remember(params, isCurrentTabIncognito, tab) {
        mutableListOf<MenuRowData>().apply {
            val primaryLabel: String? = params.titleOrAltText
            val secondaryLabel: String? = params.linkUri?.toString()
            if (primaryLabel != null || secondaryLabel != null) {
                add(MenuRowData.forHeader(primaryLabel, secondaryLabel))
                add(MenuRowData.forSeparator())
            }

            params.linkUri?.let {
                if (!isCurrentTabIncognito) {
                    add(MenuRowData.forAction(R.string.menu_open_in_new_tab))
                }

                add(MenuRowData.forAction(R.string.menu_open_in_new_incognito_tab))
                add(MenuRowData.forAction(R.string.menu_copy_link_address))
            }

            params.linkText?.let {
                add(MenuRowData.forAction(R.string.menu_copy_link_text))
            }

            if (params.canDownload) {
                val downloadStringResource = when {
                    params.isImage -> R.string.menu_download_image
                    params.isVideo -> R.string.menu_download_video
                    params.linkUri != null -> R.string.menu_download_link
                    else -> null
                }

                downloadStringResource?.let { id ->
                    add(MenuRowData.forAction(id))
                }
            }
        }
    }

    MenuContent(menuItems) { id ->
        if (tab.isDestroyed) {
            onDismissRequested()
            return@MenuContent
        }

        when (id) {
            R.string.menu_open_in_new_tab -> {
                params.linkUri?.let { linkUri ->
                    webLayerModel.switchToProfile(useIncognito = false)
                    webLayerModel.currentBrowser.loadUrl(
                        uri = linkUri,
                        inNewTab = true,
                        parentTabId = tab.guid
                    )
                }
            }

            R.string.menu_open_in_new_incognito_tab -> {
                params.linkUri?.let { linkUri ->
                    // Only keep track of the parent tab if it is also an Incognito tab.  This
                    // avoids situations where the user hits "back" to close the tab and the app
                    // can't figure out where to redirect the user.
                    val parentTabId = tab
                        .takeUnless { it.isDestroyed }
                        ?.guid
                        .takeIf { isCurrentTabIncognito }

                    webLayerModel.switchToProfile(useIncognito = true)
                    webLayerModel.currentBrowser.loadUrl(
                        uri = linkUri,
                        inNewTab = true,
                        parentTabId = parentTabId
                    )
                }
            }

            R.string.menu_copy_link_address -> {
                params.linkUri?.let { linkUri ->
                    sendToClipboard("link address", linkUri.toString())
                }
            }

            R.string.menu_copy_link_text -> {
                params.linkText?.let { linkText ->
                    sendToClipboard("link text", linkText)
                }
            }

            R.string.menu_download_image,
            R.string.menu_download_video,
            R.string.menu_download_link -> {
                tab.download(params)
            }
        }

        onDismissRequested()
    }
}
