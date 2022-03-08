package com.neeva.app.browsing

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.neeva.app.R
import org.chromium.weblayer.ContextMenuParams
import org.chromium.weblayer.Tab

/** Creates a context menu for a link that has been long-pressed. */
class ContextMenuCreator(
    private val webLayerModel: WebLayerModel,
    private val params: ContextMenuParams,
    private val tab: Tab,
    private val context: Context
) : View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {
    enum class MenuIds {
        COPY_LINK_URI,
        COPY_LINK_TEXT,
        DOWNLOAD_IMAGE,
        DOWNLOAD_VIDEO,
        DOWNLOAD_LINK,
        OPEN_IN_NEW_TAB,
        OPEN_IN_NEW_INCOGNITO_TAB
    }

    private val isCurrentTabIncognito: Boolean = webLayerModel.currentBrowser.isIncognito

    override fun onCreateContextMenu(
        menu: ContextMenu,
        view: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        // Set up the menu header.
        LayoutInflater.from(context)
            .inflate(R.layout.menu_header, null, false)
            .apply {
                val titleView = findViewById<TextView>(R.id.title)
                params.titleOrAltText
                    ?.let { titleView.text = it }
                    ?: run { titleView.visibility = View.GONE }

                val linkView = findViewById<TextView>(R.id.link)
                params.linkUri
                    ?.let { linkView.text = params.linkUri.toString() }
                    ?: run { linkView.visibility = View.GONE }

                if (titleView.visibility == View.GONE && linkView.visibility == View.GONE) {
                    // There's nothing to display, so don't add the header at all.
                    return@apply
                } else {
                    menu.setHeaderView(this)
                }

                // If only one or the other is visible, hide the space in between.
                if (titleView.visibility == View.GONE || linkView.visibility == View.GONE) {
                    (titleView.layoutParams as? ViewGroup.MarginLayoutParams)?.bottomMargin = 0
                }
            }

        if (params.linkUri != null) {
            if (!isCurrentTabIncognito) {
                menu.add(
                    Menu.NONE,
                    MenuIds.OPEN_IN_NEW_TAB.ordinal,
                    Menu.NONE,
                    R.string.menu_open_in_new_tab
                ).also { it.setOnMenuItemClickListener(this) }
            }

            menu.add(
                Menu.NONE,
                MenuIds.OPEN_IN_NEW_INCOGNITO_TAB.ordinal,
                Menu.NONE,
                R.string.menu_open_in_new_incognito_tab
            ).also { it.setOnMenuItemClickListener(this) }

            menu.add(
                Menu.NONE,
                MenuIds.COPY_LINK_URI.ordinal,
                Menu.NONE,
                R.string.menu_copy_link_address
            ).also { it.setOnMenuItemClickListener(this) }
        }

        if (!params.linkText.isNullOrEmpty()) {
            menu.add(
                Menu.NONE,
                MenuIds.COPY_LINK_TEXT.ordinal,
                Menu.NONE,
                context.getString(R.string.menu_copy_link_text)
            ).also { it.setOnMenuItemClickListener(this) }
        }

        if (params.canDownload) {
            val downloadMenuItem = when {
                params.isImage -> {
                    menu.add(
                        Menu.NONE,
                        MenuIds.DOWNLOAD_IMAGE.ordinal,
                        Menu.NONE,
                        R.string.menu_download_image
                    )
                }

                params.isVideo -> {
                    menu.add(
                        Menu.NONE,
                        MenuIds.DOWNLOAD_VIDEO.ordinal,
                        Menu.NONE,
                        R.string.menu_download_video
                    )
                }

                params.linkUri != null -> {
                    menu.add(
                        Menu.NONE,
                        MenuIds.DOWNLOAD_LINK.ordinal,
                        Menu.NONE,
                        R.string.menu_download_link
                    )
                }

                else -> null
            }
            downloadMenuItem?.setOnMenuItemClickListener(this)
        }

        view.setOnCreateContextMenuListener(null)
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        when (item.itemId) {
            MenuIds.OPEN_IN_NEW_TAB.ordinal -> {
                params.linkUri?.let {
                    webLayerModel.switchToProfile(useIncognito = false)
                    webLayerModel.currentBrowser.loadUrl(
                        uri = it,
                        inNewTab = true,
                        parentTabId = tab.guid
                    )
                }
            }

            MenuIds.OPEN_IN_NEW_INCOGNITO_TAB.ordinal -> {
                // Only keep track of the parent tab if it is also an Incognito tab.  This avoids
                // situations where the user hits "back" to close the tab and the app can't figure
                // out where to redirect the user.
                val parentTabId = tab.guid.takeIf { isCurrentTabIncognito }

                params.linkUri?.let {
                    webLayerModel.switchToProfile(useIncognito = true)
                    webLayerModel.currentBrowser.loadUrl(
                        uri = it,
                        inNewTab = true,
                        parentTabId = parentTabId
                    )
                }
            }

            MenuIds.COPY_LINK_URI.ordinal -> {
                clipboard.setPrimaryClip(
                    ClipData.newPlainText("link address", params.linkUri.toString())
                )
            }

            MenuIds.COPY_LINK_TEXT.ordinal -> {
                clipboard.setPrimaryClip(
                    ClipData.newPlainText("link text", params.linkText)
                )
            }

            MenuIds.DOWNLOAD_IMAGE.ordinal,
            MenuIds.DOWNLOAD_VIDEO.ordinal,
            MenuIds.DOWNLOAD_LINK.ordinal -> {
                tab.download(params)
            }
        }

        return true
    }
}
