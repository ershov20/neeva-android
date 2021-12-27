package com.neeva.app.browsing

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.*
import android.widget.TextView
import com.neeva.app.R
import org.chromium.weblayer.ContextMenuParams
import org.chromium.weblayer.Tab

/** Creates a context menu for a link that has been long-pressed. */
class ContextMenuCreator(
    var webLayerModel: WebLayerModel,
    var params: ContextMenuParams,
    var tab: Tab,
    var context: Context
): View.OnCreateContextMenuListener, MenuItem.OnMenuItemClickListener {
    companion object {
        private const val MENU_ID_COPY_LINK_URI = 1
        private const val MENU_ID_COPY_LINK_TEXT = 2
        private const val MENU_ID_DOWNLOAD_IMAGE = 3
        private const val MENU_ID_DOWNLOAD_VIDEO = 4
        private const val MENU_ID_DOWNLOAD_LINK = 5
        private const val MENU_ID_OPEN_IN_NEW_TAB = 6
    }

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
            val openNewTabItem =
                menu.add(Menu.NONE, MENU_ID_OPEN_IN_NEW_TAB, Menu.NONE, "Open in new tab")
            openNewTabItem.setOnMenuItemClickListener(this)

            val copyLinkUriItem =
                menu.add(Menu.NONE, MENU_ID_COPY_LINK_URI, Menu.NONE, "Copy link address")
            copyLinkUriItem.setOnMenuItemClickListener(this)
        }

        if (!params.linkText.isNullOrEmpty()) {
            val copyLinkTextItem =
                menu.add(Menu.NONE, MENU_ID_COPY_LINK_TEXT, Menu.NONE, "Copy link text")
            copyLinkTextItem.setOnMenuItemClickListener(this)
        }

        if (params.canDownload) {
            val downloadMenuItem = when {
                params.isImage -> {
                    menu.add(Menu.NONE, MENU_ID_DOWNLOAD_IMAGE, Menu.NONE, R.string.download_image)
                }

                params.isVideo -> {
                    menu.add(Menu.NONE, MENU_ID_DOWNLOAD_VIDEO, Menu.NONE, R.string.download_video)
                }

                params.linkUri != null -> {
                    menu.add(Menu.NONE, MENU_ID_DOWNLOAD_LINK, Menu.NONE, R.string.download_link)
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
            MENU_ID_OPEN_IN_NEW_TAB -> {
                params.linkUri?.let { webLayerModel.createTabFor(it) }
            }

            MENU_ID_COPY_LINK_URI -> {
                clipboard.setPrimaryClip(
                    ClipData.newPlainText("link address", params.linkUri.toString())
                )
            }

            MENU_ID_COPY_LINK_TEXT -> {
                clipboard.setPrimaryClip(
                    ClipData.newPlainText("link text", params.linkText)
                )
            }

            MENU_ID_DOWNLOAD_IMAGE, MENU_ID_DOWNLOAD_VIDEO, MENU_ID_DOWNLOAD_LINK -> {
                tab.download(params)
            }
        }

        return true
    }
}