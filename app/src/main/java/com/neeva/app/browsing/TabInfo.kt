package com.neeva.app.browsing

import android.net.Uri

/** Information required to render a Tab in the UI. */
data class TabInfo(
    val id: String,
    val url: Uri?,
    val title: String?,
    val isSelected: Boolean,
    val data: PersistedData = PersistedData(null, TabOpenType.DEFAULT)
) {
    companion object {
        const val KEY_PARENT_TAB_ID = "PARENT_TAB_ID"
        const val KEY_OPEN_TYPE = "OPEN_TYPE"
    }

    enum class TabOpenType {
        DEFAULT,
        CHILD_TAB,
        VIA_INTENT
    }

    /** Used to save info about Tab across process restarts via WebLayer. */
    data class PersistedData(
        val parentTabId: String? = null,
        val openType: TabOpenType = TabOpenType.DEFAULT
    ) {
        constructor(map: Map<String, String>) : this(
            parentTabId = map[KEY_PARENT_TAB_ID],
            openType = TabOpenType.values()
                .firstOrNull { it.name == map[KEY_OPEN_TYPE] }
                ?: TabOpenType.DEFAULT
        )

        fun toMap(): Map<String, String> {
            return mutableMapOf<String, String>().apply {
                put(KEY_OPEN_TYPE, openType.name)

                if (parentTabId != null) {
                    put(KEY_PARENT_TAB_ID, parentTabId)
                }
            }
        }
    }
}
